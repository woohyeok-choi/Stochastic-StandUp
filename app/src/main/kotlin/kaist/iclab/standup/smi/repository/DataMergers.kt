package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.common.toGeoHash
import kaist.iclab.standup.smi.data.Event
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.data.PlaceStat
import smile.math.BFGS
import smile.math.DifferentiableMultivariateFunction
import smile.math.MathEx
import java.util.*
import java.util.stream.IntStream
import kotlin.math.exp

data class SedentaryDurationEvent(
    val startTime: Long?,
    val endTime: Long?,
    val duration: Long,
    val latitude: Double,
    val longitude: Double,
    val geoHash: String
)

data class SedentaryMissionEvent(
    val placeId: String,
    val placeName: String?,
    val placeAddress: String?,
    val startTime: Long?,
    val endTime: Long?,
    val isSedentary: Boolean,
    val duration: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val missions: List<Mission> = listOf()
) {
    val incentives = missions.sumIncentives()
}

fun Collection<Mission>.sumIncentives() = sumBy { mission ->
    val incentive = mission.incentive
    if ((incentive >= 0 && mission.state == Mission.STATE_SUCCESS) || (incentive < 0 && mission.state == Mission.STATE_FAILURE))
        incentive
    else
        0
}

fun Collection<Mission>.cluster() : Map<String, List<Mission>> =
    fold(mutableMapOf<String, MutableList<Mission>>()) { acc, mission ->
        val latitude = mission.latitude
        val longitude = mission.longitude
        val geoHash = (latitude to longitude).toGeoHash(8) ?: return@fold acc
        if (acc.containsKey(geoHash)) {
            acc[geoHash]?.add(mission)
        } else {
            acc[geoHash] = mutableListOf(mission)
        }
        return@fold acc
    }

fun Collection<Mission>.calculateIncentive() : Int {
    return 0
}

fun Collection<Event>.toDurationEvents(fromTime: Long, toTime: Long) : List<SedentaryDurationEvent> {
    val subEvents = filter { event ->
        val timestamp = event.timestamp
        timestamp in (fromTime until toTime)
    }.sortedBy { it.timestamp }

    return subEvents.foldIndexed(mutableListOf()) { index, acc, event ->
        val timestamp = event.timestamp
        if (timestamp < 0) return@foldIndexed acc
        /**
         * If the first item is an exiting event,
         * it means that an entering event is before the fromTime.
         */
        if (index == 0) {
            if (!event.isEntered) {
                val duration = timestamp - fromTime
                acc.add(
                    SedentaryDurationEvent(
                        startTime = fromTime,
                        endTime = timestamp.coerceIn(fromTime, toTime),
                        duration = duration,
                        latitude = event.latitude,
                        longitude = event.longitude,
                        geoHash = event.geoHash
                    )
                )
            }
            /**
             * If the last item is an entered event,
             * it means that an exiting event is after the toTime (or, not existed)
             */
        } else if (index == subEvents.lastIndex) {
            if (event.isEntered) {
                val duration = toTime - timestamp
                acc.add(
                    SedentaryDurationEvent(
                        startTime = timestamp.coerceIn(fromTime, toTime),
                        endTime = toTime,
                        duration = duration,
                        latitude = event.latitude,
                        longitude = event.longitude,
                        geoHash = event.geoHash
                    )
                )
            }
        }

        val nextEvent = subEvents.getOrNull(index + 1)
        if (event.isEntered && nextEvent?.isEntered == false) {
            (nextEvent.timestamp - timestamp).also { duration ->
                acc.add(
                    SedentaryDurationEvent(
                        startTime = timestamp.coerceIn(fromTime, toTime),
                        endTime = (timestamp + duration).coerceIn(fromTime, toTime),
                        duration = duration,
                        latitude = event.latitude,
                        longitude = event.longitude,
                        geoHash = event.geoHash
                    )
                )
            }
        }
        return@foldIndexed acc
    }
}

fun Collection<Event>.toSedentaryMissionEvent(
    fromTime: Long,
    toTime: Long,
    missions: List<Mission>,
    places: List<PlaceStat>,
    prolongedSedentaryTimeMillis: Long
) : List<SedentaryMissionEvent> {
    val placesMap = places.associateBy { it.id }
    return toDurationEvents(fromTime, toTime).mapNotNull {  event ->
        val place = placesMap[event.geoHash] ?: return@mapNotNull null
        val includedMissions = missions.filter { mission ->
            mission.triggerTime in ((event.startTime ?: 0) until (event.endTime ?: Long.MAX_VALUE))
        }
        SedentaryMissionEvent(
            placeId = place.id,
            placeName = place.name,
            placeAddress = place.address,
            startTime = event.startTime,
            endTime = if (event.endTime != null && event.endTime >= System.currentTimeMillis()) null else event.endTime,
            isSedentary = event.duration >= prolongedSedentaryTimeMillis,
            duration = event.duration,
            latitude = event.latitude,
            longitude = event.longitude,
            missions = includedMissions
        )
    }
}
