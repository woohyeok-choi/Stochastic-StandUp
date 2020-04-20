package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.data.Event
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.data.PlaceStat

data class SedentaryDurationEvent(
    val startTime: Long,
    val endTime: Long?,
    val duration: Long,
    val latitude: Double,
    val longitude: Double,
    val geoHash: String
)

data class SedentaryMissionEvent(
    val place: PlaceStat,
    val event: SedentaryDurationEvent,
    val missions: List<Mission> = listOf()
)

data class IncentiveHistory(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val incentive: Int,
    val isSucceeded: Boolean
)

fun Collection<Mission>.sumIncentives() = sumBy { mission ->
    val incentive = mission.incentive

    if ((incentive >= 0 && mission.isSucceeded()) || (incentive < 0 && mission.isFailed())) {
        incentive
    } else {
        0
    }
}


fun Collection<Event>.toDurationEvents(fromTime: Long, toTime: Long) : List<SedentaryDurationEvent> {
    val subEvents = filter { event ->
        val timestamp = event.timestamp
        timestamp in (fromTime until toTime)
    }.sortedBy { it.timestamp }

    val originalEvents: List<SedentaryDurationEvent> = subEvents.foldIndexed(mutableListOf()) { index, acc, event ->
        val timestamp = event.timestamp
        if (timestamp < 0) return@foldIndexed acc
        /**
         * If the first item is an exiting event,
         * it means that an entering event is before the fromTime.
         */
        if (index == 0 && !event.isEntered) {
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
        if (index == subEvents.lastIndex && event.isEntered) {
            val duration = toTime - timestamp
            acc.add(
                SedentaryDurationEvent(
                    startTime = timestamp.coerceIn(fromTime, toTime),
                    endTime = null,
                    duration = duration,
                    latitude = event.latitude,
                    longitude = event.longitude,
                    geoHash = event.geoHash
                )
            )
        }

        val nextEvent = subEvents.getOrNull(index + 1)

        /**
         * When this event is ENTER and next event is EXIT,
         * it means normal situation for sedentary event.
         */
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

        /**
         * When this event is ENTER and next event is also ENTER,
         * it means erroneously restarting this service (or reinstalling).
         */
        if (event.isEntered && nextEvent?.isEntered == true) {
            acc.add(
                SedentaryDurationEvent(
                    startTime = timestamp.coerceIn(fromTime, toTime),
                    endTime = null,
                    duration = 0,
                    latitude = event.latitude,
                    longitude = event.longitude,
                    geoHash = event.geoHash
                )
            )
        }

        return@foldIndexed acc
    }
    val adjustedEvents = mutableListOf<SedentaryDurationEvent>()
    var startTime = -1L

    originalEvents.sortedBy { it.startTime }.forEach { event ->
        if (event.endTime == null) {
            if (startTime < 0) startTime = event.startTime.coerceIn(fromTime, toTime)
        }

        if (event.endTime != null) {
            if (startTime > 0) {
                val adjustedEvent = SedentaryDurationEvent(
                    startTime = startTime,
                    endTime = event.endTime,
                    duration = event.endTime - startTime,
                    latitude = event.latitude,
                    longitude = event.longitude,
                    geoHash = event.geoHash
                )
                adjustedEvents.add(adjustedEvent)
                startTime = -1L
            } else {
                adjustedEvents.add(event)
            }
        }
    }

    return adjustedEvents
}

fun Collection<Event>.toSedentaryMissionEvent(
    fromTime: Long,
    toTime: Long,
    missions: List<Mission>,
    places: List<PlaceStat>
) : List<SedentaryMissionEvent> {
    val placesMap = places.associateBy { it.id }
    return toDurationEvents(fromTime, toTime).mapNotNull {  event ->
        val place = placesMap[event.geoHash] ?: return@mapNotNull null
        val includedMissions = missions.filter { mission ->
            mission.triggerTime in (event.startTime until (event.endTime ?: toTime))
        }
        SedentaryMissionEvent(
            event = event,
            place = place,
            missions = includedMissions
        )
    }
}
