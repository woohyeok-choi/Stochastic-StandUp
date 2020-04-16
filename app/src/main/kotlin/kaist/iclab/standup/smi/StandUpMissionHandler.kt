package kaist.iclab.standup.smi

import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.repository.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import kotlin.math.abs

class StandUpMissionHandler(
    private val missionRepository: MissionRepository,
    private val statRepository: StatRepository,
    private val eventRepository: EventRepository,
    private val incentiveRepository: IncentiveRepository
) {
    suspend fun enterIntoStill(timestamp: Long, latitude: Double, longitude: Double) {
        eventRepository.createEvent(
            eventTime = timestamp,
            entered = true,
            latitude = latitude,
            longitude = longitude
        )

        statRepository.updateVisitEvent(latitude, longitude, timestamp)
    }

    suspend fun exitFromStill(timestamp: Long, latitude: Double, longitude: Double) {
        eventRepository.createEvent(
            eventTime = timestamp,
            entered = false,
            latitude = latitude,
            longitude = longitude
        )
    }

    suspend fun prepareMission(
        timestamp: Long,
        latitude: Double,
        longitude: Double
    ) : String? {
        return missionRepository.prepareMission(
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude
        )
    }

    suspend fun standByMission(
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        id: String
    ) {
        missionRepository.standByMission(
            id = id,
            timestamp = timestamp
        )
        val mission = missionRepository.getMission(id)

        statRepository.updateStayDuration(
            latitude = latitude,
            longitude = longitude,
            duration = mission?.prepareTime?.let { (timestamp - it).coerceAtLeast(0) } ?: RemotePrefs.minTimeForStayEvent
        )
    }

    suspend fun startMission(
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        id: String
    ): Mission? {
        val missions = missionRepository.getMissions(
            fromTime = timestamp - RemotePrefs.winSizeInMillis,
            toTime = timestamp
        ).takeLast(RemotePrefs.winSizeInNumber).filter { mission ->
            val prepareTime = mission.prepareTime
            if (prepareTime <= 0) return@filter false

            val standByTime = mission.standByTime
            if (standByTime <= 0) return@filter false

            val stayDuration = mission.standByTime - mission.prepareTime
            if (stayDuration < RemotePrefs.minTimeForStayEvent) return@filter false

            val state = mission.state

            if (state == Mission.STATE_IN_PROGRESS) {
                val dayStart = DateTime(standByTime, DateTimeZone.getDefault()).withTimeAtStartOfDay().millis
                val start = dayStart + LocalPrefs.activeStartTimeMs
                val end = dayStart + LocalPrefs.activeEndTimeMs
                return@filter dayStart in (start until end)
            }

            return@filter true
        }

        val incentive = when (RemotePrefs.incentiveMode) {
            RemotePrefs.INCENTIVE_MODE_FIXED -> RemotePrefs.defaultIncentives
            RemotePrefs.INCENTIVE_MODE_STOCHASTIC -> incentiveRepository.calculateStochasticIncentive(
                missions = missions,
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude
            )?.coerceIn(RemotePrefs.minIncentives, RemotePrefs.maxIncentives)?.let {
                it / RemotePrefs.unitIncentives * RemotePrefs.unitIncentives
            } ?: RemotePrefs.defaultIncentives
            else -> 0
        }

        missionRepository.startMission(
            id = id,
            timestamp = timestamp,
            incentive = incentive
        )

        val mission = missionRepository.getMission(id)

        statRepository.updateStayDuration(
            latitude = latitude,
            longitude = longitude,
            duration = mission?.standByTime?.let {
                (timestamp - it).coerceAtLeast(0)
            } ?: RemotePrefs.minTimeForMissionTrigger - RemotePrefs.minTimeForStayEvent
        )

        return mission
    }

    suspend fun completeMission(
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        id: String,
        isSucceeded: Boolean? = null
    ) : Mission? {
        val mission = missionRepository.getMission(id)
        val isMissionInProgress = mission?.state == Mission.STATE_IN_PROGRESS

        statRepository.updateStayDuration(
            latitude = latitude,
            longitude = longitude,
            duration = mission?.triggerTime?.let {
                (timestamp - it).coerceAtLeast(0)
            } ?: RemotePrefs.timeoutForMissionExpired
        )

        if (mission != null && isMissionInProgress && isSucceeded != null) {
            val incentive = mission.incentive
            val triggerTime = mission.triggerTime
            val fromTime = DateTime(triggerTime, DateTimeZone.getDefault()).withTimeAtStartOfDay().millis
            val dailyIncentives = missionRepository.getCompletedMissions(
                fromTime = fromTime,
                toTime = triggerTime
            ).sumIncentives()

            val availableBudget = (RemotePrefs.maxDailyBudget - abs(dailyIncentives)).coerceAtLeast(0)
            val actualIncentive = abs(incentive).coerceAtMost(availableBudget) * if(incentive >= 0) 1 else -1

            missionRepository.completeMission(
                id = id,
                timestamp = timestamp,
                isSucceeded = isSucceeded
            )

            statRepository.updateMissionResult(
                latitude = latitude,
                longitude = longitude,
                isSucceeded = isSucceeded,
                incentives = actualIncentive
            )
        }

        return mission
    }
}