package kaist.iclab.standup.smi

import kaist.iclab.standup.smi.common.AppLog
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
        AppLog.d(javaClass, "enterIntoStill(timestamp: $timestamp, latitude: $latitude, longitude: $longitude)")

        eventRepository.createEvent(
            eventTime = timestamp,
            entered = true,
            latitude = latitude,
            longitude = longitude
        )

        statRepository.updateVisitEvent(latitude, longitude, timestamp)
    }

    suspend fun exitFromStill(timestamp: Long, latitude: Double, longitude: Double) {
        AppLog.d(javaClass, "exitFromStill(timestamp: $timestamp, latitude: $latitude, longitude: $longitude)")

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
        AppLog.d(javaClass, "prepareMission(timestamp: $timestamp, latitude: $latitude, longitude: $longitude)")

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
        AppLog.d(javaClass, "standByMission(timestamp: $timestamp, latitude: $latitude, longitude: $longitude)")

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
        AppLog.d(javaClass, "startMission(timestamp: $timestamp, latitude: $latitude, longitude: $longitude)")

        val incentive = try {
            when (RemotePrefs.incentiveMode) {
                RemotePrefs.INCENTIVE_MODE_FIXED -> RemotePrefs.defaultIncentives
                RemotePrefs.INCENTIVE_MODE_STOCHASTIC -> incentiveRepository.calculateStochasticIncentive(
                    histories = getIncentiveHistories(timestamp),
                    timestamp = timestamp,
                    latitude = latitude,
                    longitude = longitude
                ) ?: RemotePrefs.defaultIncentives
                else -> 0
            }
        } catch (e: Exception) {
            AppLog.ee(javaClass, "Error occurs on incentive calculation", e)
            RemotePrefs.defaultIncentives
        }

        AppLog.d(javaClass, "startMission(): incentive=$incentive")

        val signedIncentive = if(RemotePrefs.isGainIncentive) abs(incentive) else -abs(incentive)

        missionRepository.startMission(
            id = id,
            timestamp = timestamp,
            incentive = signedIncentive
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
        val mission = missionRepository.getMission(id) ?: return null
        val isMissionInProgress = mission.state == Mission.STATE_TRIGGERED

        statRepository.updateStayDuration(
            latitude = latitude,
            longitude = longitude,
            duration = mission.triggerTime.let {
                (timestamp - it).coerceAtLeast(0)
            }
        )

        missionRepository.completeMission(
            id = id,
            timestamp = timestamp,
            isSucceeded = isSucceeded
        )

        if (isMissionInProgress && isSucceeded != null) {
            val incentive = mission.incentive
            val triggerTime = mission.triggerTime
            val fromTime = DateTime(triggerTime, DateTimeZone.getDefault()).withTimeAtStartOfDay().millis
            val dailyIncentives = missionRepository.getTriggeredMissions(
                fromTime = fromTime,
                toTime = triggerTime
            ).sumIncentives()

            val availableBudget = (RemotePrefs.maxDailyBudget - abs(dailyIncentives)).coerceAtLeast(0)
            val actualIncentive = abs(incentive).coerceIn(0, availableBudget) * if(incentive >= 0) 1 else -1

            statRepository.updateMissionResult(
                latitude = latitude,
                longitude = longitude,
                isSucceeded = isSucceeded,
                incentives = actualIncentive
            )
        }

        return mission
    }

    private suspend fun getIncentiveHistories(timestamp: Long) = missionRepository.getMissions(
            fromTime = timestamp - RemotePrefs.winSizeInMillis,
            toTime = timestamp
        ).takeLast(RemotePrefs.winSizeInNumber).mapNotNull { mission ->
            val standByTime = mission.standByTime
            if (standByTime <= 0) return@mapNotNull null

            val reactionTime = mission.reactionTime
            val triggerTime = mission.triggerTime

            if (reactionTime <= 0 || triggerTime <= 0) return@mapNotNull null

            val dayStart = DateTime(triggerTime, DateTimeZone.getDefault()).withTimeAtStartOfDay().millis
            val start = dayStart + LocalPrefs.activeStartTimeMs
            val end = dayStart + LocalPrefs.activeEndTimeMs

            return@mapNotNull if (triggerTime in (start until end)) {
                IncentiveHistory(
                    timestamp = triggerTime,
                    latitude = mission.latitude,
                    longitude = mission.longitude,
                    incentive = if (mission.isStandBy()) 0 else mission.incentive,
                    isSucceeded = if (mission.isStandBy()) true else mission.isSucceeded()
                )
            } else {
                null
            }
        }
}