package kaist.iclab.standup.smi.ui.dashboard

import androidx.lifecycle.*
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.Formatter
import kaist.iclab.standup.smi.common.Status
import kaist.iclab.standup.smi.common.sumByLong
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DashboardViewModel(
    private val eventRepository: EventRepository,
    private val missionRepository: MissionRepository
) : BaseViewModel<DashboardNavigator>() {
    private val ioContext = viewModelScope.coroutineContext + Dispatchers.IO

    fun loadData(dateTime: DateTime) = viewModelScope.launch(ioContext) {
        loadData(dateTime, false)
    }

    fun refresh() = viewModelScope.launch(ioContext) {
        currentDateTime.value?.let { loadData(it, true) }
    }

    fun loadChart(flag: Int) {
        if (currentChartType.value != flag) currentChartType.value = flag
    }

    private suspend fun loadData(dateTime: DateTime, isForced: Boolean) {
        if (!isForced && currentDateTime.value == dateTime) return
        currentDateTime.postValue(dateTime)

        dailyLoadStatus.postValue(Status.loading())
        weeklyLoadStatus.postValue(Status.loading())

        try {
            loadDailyData(dateTime)
            dailyLoadStatus.postValue(Status.success())

            loadWeeklyData(dateTime)
            weeklyLoadStatus.postValue(Status.success())
        } catch (e: Exception) {
            dailyLoadStatus.postValue(Status.failure(e))
            weeklyLoadStatus.postValue(Status.failure(e))

            navigator?.navigateError(e)
        }
    }

    val currentChartType: MutableLiveData<Int> = MutableLiveData(-1)
    val currentDateTime: MutableLiveData<DateTime> = MutableLiveData()

    private val dailySedentaryEvents: MutableLiveData<List<SedentaryDurationEvent>> =
        MutableLiveData()
    private val dailyMissions: MutableLiveData<List<Mission>> = MutableLiveData()

    private val weeklySedentaryEvents: MutableLiveData<Map<DateTime, List<SedentaryDurationEvent>>> =
        MutableLiveData()
    private val weeklyMissions: MutableLiveData<Map<DateTime, List<Mission>>> = MutableLiveData()

    val dailyLoadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())
    val weeklyLoadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())

    private suspend fun loadDailyData(dateTime: DateTime) {
        val (from, to) = dateTime.let { curTime ->
            val timestamp = System.currentTimeMillis()
            val dayStart = curTime.withTimeAtStartOfDay().millis
            val start = dayStart + LocalPrefs.activeStartTimeMs
            val end = (dayStart + LocalPrefs.activeEndTimeMs).coerceAtMost(timestamp)
            start to end
        }

        val events = eventRepository.getEvents(from, to).toDurationEvents(from, to)
        val missions = missionRepository.getCompletedMissions(
            fromTime = from,
            toTime = to
        )

        dailySedentaryEvents.postValue(events)
        dailyMissions.postValue(missions)
    }

    private suspend fun loadWeeklyData(dateTime: DateTime) {
        val timeRanges = (0..7).map { len ->
            val curTimestamp = System.currentTimeMillis()
            val dayStart = dateTime.withTimeAtStartOfDay().minusDays(len).millis
            val start = dayStart + LocalPrefs.activeStartTimeMs
            val end = (dayStart + LocalPrefs.activeEndTimeMs).coerceAtMost(curTimestamp)
            start to end
        }

        val fromTime = timeRanges.minBy { it.first }?.first
        val toTime = timeRanges.maxBy { it.second }?.second

        if (fromTime == null || toTime == null) {
            weeklySedentaryEvents.postValue(mapOf())
            weeklyMissions.postValue(mapOf())
        } else {
            val events = eventRepository.getEvents(fromTime, toTime).let { events ->
                timeRanges.associate { (from, to) ->
                    DateTime(from, DateTimeZone.getDefault()) to events.toDurationEvents(from, to)
                }
            }
            val missions = missionRepository.getCompletedMissions(fromTime, toTime).let { missions ->
                timeRanges.associate { (from, to) ->
                    DateTime(
                        from,
                        DateTimeZone.getDefault()
                    ) to missions.filter { it.triggerTime in (from until to) }
                }
            }

            weeklySedentaryEvents.postValue(events)
            weeklyMissions.postValue(missions)
        }
    }

    private val weeklySedentaryTotalDuration =
        weeklySedentaryEvents.map { data ->
            data?.mapValues { (_, events) ->
                TimeUnit.MILLISECONDS.toMinutes(events.sumByLong { it.duration })
            } ?: mapOf()
        }

    private val weeklySedentaryAvgDuration =
        weeklySedentaryEvents.map { data ->
            data?.mapValues { (_, events) ->
                TimeUnit.MILLISECONDS.toMinutes(events.map { it.duration }.average().toLong())
            } ?: mapOf()
        }

    private val weeklyNumProlongedSedentariness =
        weeklySedentaryEvents.map { data ->
            data?.mapValues { (_, events) ->
                events.filter {
                    it.duration >= RemotePrefs.maxProlongedSedentaryTime
                }.size.toLong()
            } ?: mapOf()
        }

    private val weeklyIncentive =
        weeklyMissions.map { data ->
            data.mapValues { (_, missions) ->
                val incentive = missions.sumIncentives()
                if (incentive >= 0) {
                    incentive.coerceAtMost(RemotePrefs.maxDailyBudget)
                } else {
                    (RemotePrefs.maxDailyBudget - abs(incentive)).coerceAtLeast(0)
                }
            }
        }

    val dailyNumProlongedSedentariness =
        dailySedentaryEvents.map { data ->
            data?.filter { it.duration >= RemotePrefs.maxProlongedSedentaryTime }?.size ?: 0
        }

    val dailyNumProlongedSedentarinessRate =
        dailyNumProlongedSedentariness.map { num ->
            Formatter.percentage(num, RemotePrefs.maxDailyNumProlongedSedentariness).toFloat() / 100
        }

    val dailyTotalSedentaryMinutes =
        dailySedentaryEvents.map { data ->
            data?.sumByLong { it.duration }?.let { TimeUnit.MILLISECONDS.toMinutes(it) } ?: 0
        }

    val dailyTotalSedentaryMinutesRate =
        dailyTotalSedentaryMinutes.map { minutes ->
            val activeDuration = LocalPrefs.activeEndTimeMs - LocalPrefs.activeStartTimeMs
            Formatter.percentage(minutes, TimeUnit.MILLISECONDS.toMinutes(activeDuration))
                .toFloat() / 100
        }

    val dailyAvgSedentaryMinutes =
        dailySedentaryEvents.map { data ->
            data?.map { it.duration }?.average()?.toLong()?.let {
                TimeUnit.MILLISECONDS.toMinutes(it)
            } ?: 0
        }

    val dailyAvgSedentaryMinutesRate =
        dailyAvgSedentaryMinutes.map { minutes ->
            Formatter.percentage(
                minutes,
                TimeUnit.MILLISECONDS.toMinutes(RemotePrefs.maxStayTimePerSession)
            ).toFloat() / 100
        }

    val dailyTotalIncentives =
        dailyMissions.map { data ->
            data?.sumIncentives()?.let { incentive ->
                if (incentive >= 0) {
                    incentive.coerceAtMost(RemotePrefs.maxDailyBudget)
                } else {
                    (RemotePrefs.maxDailyBudget - abs(incentive)).coerceAtLeast(0)
                }
            } ?: 0
        }

    val dailyNumMissionsReceived =
        dailyMissions.map { data -> data?.size ?: 0 }

    val dailyNumMissionsSucceeded =
        dailyMissions.map { data -> data?.filter { it.state == Mission.STATE_SUCCESS }?.size ?: 0 }

    fun weeklyChartData(): LiveData<Pair<Map<DateTime, Pair<Long, Int>>, Int>> {
        val liveData: MediatorLiveData<Pair<Map<DateTime, Pair<Long, Int>>, Int>> =
            MediatorLiveData()
        var durations: Map<DateTime, Long>? = null
        var incentives: Map<DateTime, Int>? = null

        fun merge(d: Map<DateTime, Long>?, i: Map<DateTime, Int>?, type: Int?) {
            if (d == null || i == null || type == null) return
            val dateTimes = d.keys.union(i.keys)

            val result = dateTimes.associateWith { dateTime ->
                val duration = d[dateTime] ?: 0
                val incentive = i[dateTime] ?: 0
                duration to incentive
            }
            liveData.postValue(result to type)
        }

        liveData.addSource(weeklyIncentive) {
            incentives = it
            merge(durations, incentives, currentChartType.value)
        }

        liveData.addSource(currentChartType) { type ->
            when (type) {
                DashboardFragment.CHART_TYPE_TOTAL_SEDENTARY_TIME -> {
                    liveData.removeSource(weeklySedentaryAvgDuration)
                    liveData.removeSource(weeklyNumProlongedSedentariness)
                    liveData.addSource(weeklySedentaryTotalDuration) {
                        durations = it
                        merge(durations, incentives, type)
                    }
                }
                DashboardFragment.CHART_TYPE_AVG_SEDENTARY_TIME -> {
                    liveData.removeSource(weeklySedentaryTotalDuration)
                    liveData.removeSource(weeklyNumProlongedSedentariness)
                    liveData.addSource(weeklySedentaryAvgDuration) {
                        durations = it
                        merge(durations, incentives, type)
                    }
                }
                DashboardFragment.CHART_TYPE_NUM_PROLONGED_SEDENTARINESS -> {
                    liveData.removeSource(weeklySedentaryTotalDuration)
                    liveData.removeSource(weeklySedentaryAvgDuration)
                    liveData.addSource(weeklyNumProlongedSedentariness) {
                        durations = it
                        merge(durations, incentives, type)
                    }
                }
                else -> {
                    liveData.removeSource(weeklySedentaryTotalDuration)
                    liveData.removeSource(weeklySedentaryAvgDuration)
                    liveData.removeSource(weeklyNumProlongedSedentariness)
                }
            }
        }
        return liveData
    }
}