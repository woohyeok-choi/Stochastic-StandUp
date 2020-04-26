package kaist.iclab.standup.smi.repository

import com.google.firebase.firestore.CollectionReference
import kaist.iclab.standup.smi.common.toGeoHash
import kaist.iclab.standup.smi.common.toISODateTime
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.data.Missions
import java.util.*

class MissionRepository(
    private val reference: () -> CollectionReference?
) {
    suspend fun getMission(id: String): Mission? =
        reference.invoke()?.let { reference -> Mission.get(reference, id) }

    suspend fun getTriggeredMissions(
        fromTime: Long,
        toTime: Long,
        geoHash: String? = null
    ): List<Mission> =
        reference.invoke()?.let { reference ->
            Mission.select(
                ref = reference,
                orderBy = Missions.triggerTime
            ) {
                Missions.triggerTime greaterThanOrEqualTo fromTime
                Missions.triggerTime lessThan toTime
                Missions.state isOneOf listOf(
                    Mission.STATE_FAILURE,
                    Mission.STATE_SUCCESS,
                    Mission.STATE_TRIGGERED
                )
                if (!geoHash.isNullOrBlank()) Missions.geoHash equalTo geoHash
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
            this.state = Mission.STATE_PREPARED

            if (!latitude.isNaN() && !longitude.isNaN()) {
                this.latitude = latitude
                this.longitude = longitude
                this.geoHash = (latitude to longitude).toGeoHash() ?: ""
            }
        }
    }

    suspend fun standByMission(
        id: String,
        timestamp: Long
    ) = reference.invoke()?.let { reference ->
        Mission.update(reference, id) {
            this.standByTime = timestamp
            this.state = Mission.STATE_STAND_BY
        }
    }

    suspend fun startMission(
        id: String,
        timestamp: Long,
        incentive: Int
    ) = reference.invoke()?.let { reference ->
        Mission.update(reference, id) {
            this.triggerTime = timestamp
            this.state = Mission.STATE_TRIGGERED
            this.incentive = incentive
        }
    }

    suspend fun completeMission(
        id: String,
        timestamp: Long,
        isSucceeded: Boolean? = null
    ) = reference.invoke()?.let { reference ->
        if (isSucceeded != null) {
            Mission.update(reference, id) {
                this.reactionTime = timestamp
                this.state = if (isSucceeded) Mission.STATE_SUCCESS else Mission.STATE_FAILURE
            }
        } else {
            Mission.update(reference, id) {
                this.reactionTime = timestamp
            }
        }
    }
}