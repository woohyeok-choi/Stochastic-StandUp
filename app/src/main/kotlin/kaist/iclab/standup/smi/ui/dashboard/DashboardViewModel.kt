package kaist.iclab.standup.smi.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.*
import kaist.iclab.standup.smi.data.Event
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.repository.*
import kaist.iclab.standup.smi.ui.config.config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DashboardViewModel(
    private val context: Context,
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

    val currentDateTime: MutableLiveData<DateTime> = MutableLiveData()

    private val dailyEvents: MutableLiveData<List<Event>> = MutableLiveData()
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

        val events = eventRepository.getEvents(from, to)
        val durationEvents = events.toDurationEvents(from ,to)
        val missions = missionRepository.getTriggeredMissions(
            fromTime = from,
            toTime = to
        )

        AppLog.d(javaClass, "events = $events / duration = $durationEvents")

        dailyEvents.postValue(events)
        dailySedentaryEvents.postValue(durationEvents)
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
            val timeZone = DateTimeZone.getDefault()
            val events = eventRepository.getEvents(fromTime, toTime).let { events ->
                timeRanges.associate { (from, to) ->
                    DateTime(from, timeZone) to events.toDurationEvents(from, to)
                }
            }
            val missions = missionRepository.getTriggeredMissions(fromTime, toTime).let { missions ->
                timeRanges.associate { (from, to) ->
                    DateTime(from, timeZone) to missions.filter { it.triggerTime in (from until to) }
                }
            }

            weeklySedentaryEvents.postValue(events)
            weeklyMissions.postValue(missions)
        }
    }

    val profileImageUrl = liveData {
        emit(
            GoogleSignIn.getLastSignedInAccount(context)?.photoUrl?.toString()
        )
    }

    /**
     * Daily Stats
     * 1. Visualized stats
     * 1.1 Mission-related stats
     * - # missions triggered
     * - # missions succeeded
     * - # ratio of success
     * 1.2. Incentive-related stats
     * - maximum incentive
     * - obtained incentive
     * - ratio of incentive
     *
     * 2. Text stats
     * 2.1. Total sedentary time
     * 2.2. Average sedentary time (with confidence interval)
     * 2.3. # stand-up
     *
     */

    val dailyNumMissionsTriggered = dailyMissions.map { data -> data?.size ?: 0 }
    val dailyNumMissionsSuccess = dailyMissions.map { data ->
        data?.filter { it.isSucceeded() }?.size ?: 0
    }
    val dailyMissionSuccessRate = dailyMissions.map { data ->
        val nSuccess = data?.filter { it.isSucceeded() } ?.size ?: 0
        val nMission = data.size
        if (nMission == 0) 0F else nSuccess.toFloat() / nMission
    }

    val dailyIncentiveTotal = liveData(ioContext) { emit(RemotePrefs.maxDailyBudget) }
    val dailyIncentiveObtained = dailyMissions.map { data ->
        val isGainMode = RemotePrefs.isGainIncentive
        val maxBudget = RemotePrefs.maxDailyBudget

        val incentives = data?.sumIncentives() ?: 0

        if (isGainMode) {
            incentives.coerceIn(0, maxBudget)
        } else {
            (maxBudget - incentives).coerceIn(0, maxBudget)
        }
    }
    val dailyIncentiveRate = dailyIncentiveObtained.map { it.toFloat() / RemotePrefs.maxDailyBudget }

    val dailyTotalSedentaryMillis = dailySedentaryEvents.map { data ->
            data?.sumByLong { it.duration } ?: 0
        }
    val dailyAvgSedentaryMillis = dailySedentaryEvents.map { data ->
        data?.map { it.duration }?.average()?.toLong() ?: 0
    }
    val dailyTotalNumStandUp = dailyEvents.map { data ->
        data?.map { !it.isEntered }?.size ?: 0
    }

    /**
     * Weekly Stats
     */
    private val weeklyAvgSedentaryMillis = weeklySedentaryEvents.map { data ->
        data?.mapValues { (_, events) ->
            val durations = events.map { it.duration }.filter {
                it > RemotePrefs.minTimeForStayEvent
            }
            val mean = durations.average().toLong()
            val confInt = durations.confidenceInterval(alpha = 0.05, isSample = true).toLong()
            val (lower, upper) = (mean - confInt).coerceAtLeast(0) to (mean + confInt)
            Triple(mean, lower, upper)
        } ?: mapOf()
    }

    private val weeklyIncentiveObtained =
        weeklyMissions.map { data ->
            data?.mapValues { (_, missions) ->
                val isGainMode = RemotePrefs.isGainIncentive
                val maxBudget = RemotePrefs.maxDailyBudget
                val incentives = missions.sumIncentives()

                if (isGainMode) {
                    incentives.coerceIn(0, maxBudget)
                } else {
                    (maxBudget - incentives).coerceIn(0, maxBudget)
                }
            } ?: mapOf()
        }

    val weeklyChartData = MediatorLiveData<Map<DateTime, Pair<Triple<Long, Long, Long>, Int>>>().apply {
        addSource(weeklyAvgSedentaryMillis) { millis ->
            val incentives = weeklyIncentiveObtained.value
            if (millis != null && incentives != null) {
                val keys = millis.keys.union(incentives.keys)
                val mergedData = keys.associateWith { time ->
                    val sedentaryMillis = millis[time] ?: Triple(0L, 0L, 0L)
                    val incentive = incentives[time] ?: 0
                    sedentaryMillis to incentive
                }
                postValue(mergedData)
            }
        }

        addSource(weeklyIncentiveObtained) { incentives ->
            val millis = weeklyAvgSedentaryMillis.value
            if (millis != null && incentives != null) {
                val keys = millis.keys.union(incentives.keys)
                val mergedData = keys.associateWith { time ->
                    val sedentaryMillis = millis[time] ?: Triple(0L, 0L, 0L)
                    val incentive = incentives[time] ?: 0
                    sedentaryMillis to incentive
                }
                postValue(mergedData)
            }
        }
    }
}