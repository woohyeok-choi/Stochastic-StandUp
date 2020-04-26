package kaist.iclab.standup.smi

import android.util.Log
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
    }

    suspend fun startMission(
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        id: String
    ): Mission? {
        AppLog.d(javaClass, "startMission(timestamp: $timestamp, latitude: $latitude, longitude: $longitude)")

        val incentive = try {
            when (BuildConfig.INCENTIVE_MODE) {
                BuildConfig.INCENTIVE_MODE_FIXED -> RemotePrefs.defaultIncentives
                BuildConfig.INCENTIVE_MODE_STOCHASTIC -> incentiveRepository.calculateStochasticIncentive(
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

        val signedIncentive = if(BuildConfig.IS_GAIN_INCENTIVE) abs(incentive) else -abs(incentive)

        missionRepository.startMission(
            id = id,
            timestamp = timestamp,
            incentive = signedIncentive
        )

        return missionRepository.getMission(id)
    }

    suspend fun completeMission(
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        id: String,
        isSucceeded: Boolean? = null
    ) : Mission? {
        val mission = missionRepository.getMission(id) ?: return null
        val dayStart = DateTime(timestamp, DateTimeZone.getDefault()).withTimeAtStartOfDay().millis
        val isMissionInProgress = mission.state == Mission.STATE_TRIGGERED

        if (isMissionInProgress && isSucceeded != null) {
            val incentive = mission.incentive
            val isGainMode = BuildConfig.IS_GAIN_INCENTIVE
            val triggeredMissions = missionRepository.getTriggeredMissions(dayStart, timestamp)
            val totalIncentives = triggeredMissions.sumIncentives()
            val maxBudget = RemotePrefs.maxDailyBudget
            val availableBudget = (maxBudget - abs(totalIncentives)).coerceAtLeast(0)
            val realIncentive = abs(incentive).coerceIn(0, availableBudget) * (if(isGainMode) 1 else -1)

            Log.d(javaClass.name, "$totalIncentives, $maxBudget, $availableBudget, $realIncentive")

            statRepository.updateMissionResult(
                latitude = latitude,
                longitude = longitude,
                isSucceeded = isSucceeded,
                incentives = realIncentive
            )
        }

        missionRepository.completeMission(
            id = id,
            timestamp = timestamp,
            isSucceeded = isSucceeded
        )

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