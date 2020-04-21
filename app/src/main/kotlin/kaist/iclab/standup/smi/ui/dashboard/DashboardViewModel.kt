package kaist.iclab.standup.smi.ui.dashboard

import android.content.Context
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.Status
import kaist.iclab.standup.smi.common.confidenceInterval
import kaist.iclab.standup.smi.common.sumByLong
import kaist.iclab.standup.smi.data.Event
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
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
    private val dailyMissionEvents: MutableLiveData<List<SedentaryMissionEvent>> =
        MutableLiveData()

    private val weeklyMissionEvents: MutableLiveData<Map<DateTime, List<SedentaryMissionEvent>>> =
        MutableLiveData()

    val dailyLoadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())
    val weeklyLoadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())

    private suspend fun loadDailyData(dateTime: DateTime) {
        val (from, to) = dateTime.let { curTime ->
            val dayStart = curTime.withTimeAtStartOfDay().millis
            val start = dayStart + LocalPrefs.activeStartTimeMs
            val end = (dayStart + LocalPrefs.activeEndTimeMs)
            start to end
        }

        val events = eventRepository.getEvents(from, to)
        val missions = missionRepository.getTriggeredMissions(
            fromTime = from,
            toTime = to
        )
        val mergedEvents = events.toSedentaryMissionEvent(fromTime = from, toTime = to, missions = missions)

        dailyEvents.postValue(events)
        dailyMissionEvents.postValue(mergedEvents)
    }

    private suspend fun loadWeeklyData(dateTime: DateTime) {
        val timeRanges = (0..7).map { len ->
            val dayStart = dateTime.withTimeAtStartOfDay().minusDays(len).millis
            val start = dayStart + LocalPrefs.activeStartTimeMs
            val end = (dayStart + LocalPrefs.activeEndTimeMs)
            start to end
        }

        val fromTime = timeRanges.minBy { it.first }?.first
        val toTime = timeRanges.maxBy { it.second }?.second

        if (fromTime == null || toTime == null) {
            weeklyMissionEvents.postValue(mapOf())
        } else {
            val timeZone = DateTimeZone.getDefault()
            val events = eventRepository.getEvents(fromTime, toTime)
            val missions = missionRepository.getTriggeredMissions(fromTime, toTime)

            val mergedEvents = timeRanges.associate { (from, to) ->
                DateTime(from, timeZone) to events.toSedentaryMissionEvent(fromTime = from, toTime = to, missions = missions)
            }

            weeklyMissionEvents.postValue(mergedEvents)
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

    val dailyNumMissionsTriggered = dailyMissionEvents.map { data ->
        data?.sumBy { it.missions.size } ?: 0
    }
    val dailyNumMissionsSuccess = dailyMissionEvents.map { data ->
        data?.sumBy { datum -> datum.missions.filter { it.isSucceeded() }.size } ?: 0
    }
    val dailyMissionSuccessRate = dailyMissionEvents.map { data ->
        val nSuccess = data?.sumBy { datum -> datum.missions.filter { it.isSucceeded() }.size } ?: 0
        val nMission = data?.sumBy { it.missions.size } ?: 0
        if (nMission == 0) 0F else nSuccess.toFloat() / nMission
    }

    val dailyIncentiveTotal = liveData(ioContext) { emit(RemotePrefs.maxDailyBudget) }
    val dailyIncentiveObtained = dailyMissionEvents.map { data ->
        val isGainMode = RemotePrefs.isGainIncentive
        val maxBudget = RemotePrefs.maxDailyBudget

        val incentives = data?.sumBy { it.missions.sumIncentives() } ?: 0

        if (isGainMode) {
            incentives.coerceIn(0, maxBudget)
        } else {
            (maxBudget - abs(incentives)).coerceIn(0, maxBudget)
        }
    }
    val dailyIncentiveRate =
        dailyIncentiveObtained.map { it.toFloat() / RemotePrefs.maxDailyBudget }

    val dailyTotalSedentaryMillis = dailyMissionEvents.map { data ->
        data?.sumByLong { it.event.duration } ?: 0
    }
    val dailyAvgSedentaryMillis = dailyMissionEvents.map { data ->
        data?.map { it.event.duration }?.average()?.toLong() ?: 0
    }
    val dailyTotalNumStandUp = dailyEvents.map { data ->
        data?.filter { !it.isEntered }?.size ?: 0
    }

    /**
     * Weekly Stats
     */
    private val weeklyAvgSedentaryMillis = weeklyMissionEvents.map { data ->
        data?.mapValues { (_, events) ->
            val durations = events.map { it.event.duration }
            val mean = durations.average().toLong()
            val confInt = durations.confidenceInterval(alpha = 0.05, isSample = true).toLong()
            val (lower, upper) = (mean - confInt).coerceAtLeast(0) to (mean + confInt)
            Triple(mean, lower, upper)
        } ?: mapOf()
    }

    private val weeklyIncentiveObtained = weeklyMissionEvents.map { data ->
            data?.mapValues { (_, events) ->
                val isGainMode = RemotePrefs.isGainIncentive
                val maxBudget = RemotePrefs.maxDailyBudget
                val incentives = events.sumBy { it.missions.sumIncentives() }

                if (isGainMode) {
                    incentives.coerceIn(0, maxBudget)
                } else {
                    (maxBudget - abs(incentives)).coerceIn(0, maxBudget)
                }
            } ?: mapOf()
        }

    val weeklyChartData =
        MediatorLiveData<Map<DateTime, Pair<Triple<Long, Long, Long>, Int>>>().apply {
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