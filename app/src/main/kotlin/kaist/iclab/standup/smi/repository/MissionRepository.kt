package kaist.iclab.standup.smi.repository

import com.google.firebase.firestore.CollectionReference
import kaist.iclab.standup.smi.common.toISODateTime
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.data.Missions
import java.util.*

class MissionRepository(
    private val reference: () -> CollectionReference?
) {
    suspend fun getMission(id: String): Mission? =
        reference.invoke()?.let { reference -> Mission.get(reference, id) }

    suspend fun getCompletedMissions(
        fromTime: Long,
        toTime: Long
    ): List<Mission> =
        reference.invoke()?.let { reference ->
            Mission.select(
                ref = reference,
                orderBy = Missions.triggerTime
            ) {
                Missions.triggerTime greaterThanOrEqualTo fromTime
                Missions.triggerTime lessThan toTime
                Missions.state isOneOf listOf(Mission.STATE_FAILURE, Mission.STATE_SUCCESS)
            }
        } ?: listOf()

    suspend fun getMissions(
        fromTime: Long,
        toTime: Long
    ): List<Mission> =
        reference.invoke()?.let { reference ->
            Mission.select(
                ref = reference,
                orderBy = Missions.standByTime
            ) {
                Missions.standByTime greaterThanOrEqualTo fromTime
                Missions.standByTime lessThan toTime
            }
        } ?: listOf()

    suspend fun prepareMission(
        timestamp: Long,
        latitude: Double,
        longitude: Double
    ): String? = reference.invoke()?.let { reference ->
        Mission.create(reference, timestamp.toISODateTime()) {
            this.offsetMs = TimeZone.getDefault().rawOffset
            this.prepareTime = timestamp
            this.state = Mission.STATE_IN_PROGRESS

            if (!latitude.isNaN() && !longitude.isNaN()) {
                this.latitude = latitude
                this.longitude = longitude
            }
        }
    }

    suspend fun standByMission(
        id: String,
        timestamp: Long
    ) = reference.invoke()?.let { reference ->
        Mission.update(reference, id) {
            this.standByTime = timestamp
        }
    }

    suspend fun startMission(
        id: String,
        timestamp: Long,
        incentive: Int
    ) = reference.invoke()?.let { reference ->
        Mission.update(reference, id) {
            this.triggerTime = timestamp
            this.incentive = incentive
        }
    }

    suspend fun completeMission(
        id: String,
        timestamp: Long,
        isSucceeded: Boolean
    ) = reference.invoke()?.let { reference ->
        Mission.update(reference, id) {
            this.reactionTime = timestamp
            this.state = if (isSucceeded) Mission.STATE_SUCCESS else Mission.STATE_FAILURE
        }
    }
}