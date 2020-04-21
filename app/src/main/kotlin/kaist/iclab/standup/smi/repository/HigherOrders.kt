package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.common.toGeoHash
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
    val place: PlaceStat?,
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


private fun Collection<Event>.toDurationEvents(fromTime: Long, toTime: Long, curTime: Long) : List<SedentaryDurationEvent> {
    val adjustToTime = toTime.coerceAtMost(curTime)
    val curTimeIsAfterToTime = curTime >= toTime

    val subEvents = filter { event ->
        val timestamp = event.timestamp
        timestamp in (fromTime until adjustToTime)
    }.sortedBy { it.timestamp }

    val originalEvents: List<SedentaryDurationEvent> = subEvents.foldIndexed(mutableListOf()) { index, acc, event ->
        val timestamp = event.timestamp
        if (timestamp < 0) return@foldIndexed acc

        val latitude = event.latitude
        val longitude = event.longitude
        val geoHash = (latitude to longitude).toGeoHash() ?: ""
        /**
         * If the first item is an exiting event,
         * it means that an entering event is before the fromTime.
         */
        if (index == 0 && !event.isEntered) {
            val duration = timestamp - fromTime
            acc.add(
                SedentaryDurationEvent(
                    startTime = fromTime,
                    endTime = timestamp.coerceIn(fromTime, adjustToTime),
                    duration = duration,
                    latitude = latitude,
                    longitude = longitude,
                    geoHash = geoHash
                )
            )
        }

        /**
         * If the last item is an entered event,
         * it means that an exiting event is after the toTime (or, not existed)
         */
        if (index == subEvents.lastIndex && event.isEntered) {
            val duration = adjustToTime - timestamp
            acc.add(
                SedentaryDurationEvent(
                    startTime = timestamp.coerceIn(fromTime, adjustToTime),
                    endTime = if (curTimeIsAfterToTime) toTime else Long.MAX_VALUE,
                    duration = duration,
                    latitude = latitude,
                    longitude = longitude,
                    geoHash = geoHash
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
                        startTime = timestamp.coerceIn(fromTime, adjustToTime),
                        endTime = (timestamp + duration).coerceIn(fromTime, adjustToTime),
                        duration = duration,
                        latitude = latitude,
                        longitude = longitude,
                        geoHash = geoHash
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
                    startTime = timestamp.coerceIn(fromTime, adjustToTime),
                    endTime = null,
                    duration = 0,
                    latitude = latitude,
                    longitude = longitude,
                    geoHash = geoHash
                )
            )
        }

        return@foldIndexed acc
    }

    val adjustedEvents = mutableListOf<SedentaryDurationEvent>()

    var adjEvent: SedentaryDurationEvent? = null
    for (event in originalEvents.sortedBy { it.startTime }) {
        if (event.endTime == null && adjEvent == null) {
            adjEvent = event
        }

        if (event.endTime != null) {
            if (adjEvent != null) {
                val startTime = adjEvent.startTime
                val newEvent = event.copy(
                    startTime = startTime,
                    duration = event.endTime.coerceAtMost(adjustToTime) - startTime
                )
                adjustedEvents.add(newEvent)
                adjEvent = null
            } else {
                adjustedEvents.add(event)
            }
        }
    }
    if (adjEvent != null) {
        adjustedEvents.add(adjEvent.copy(
            endTime = Long.MAX_VALUE,
            duration = adjustToTime - adjEvent.startTime
        ))
    }

    return adjustedEvents.filter { it.duration > 0 }
}

fun Collection<Event>.toSedentaryMissionEvent(
    fromTime: Long,
    toTime: Long,
    missions: List<Mission> = listOf(),
    places: List<PlaceStat> = listOf()
) : List<SedentaryMissionEvent> {
    val curTime = System.currentTimeMillis()
    val placesMap = places.associateBy { it.id }

    return toDurationEvents(fromTime, toTime, curTime).map { event ->
        val place = placesMap[event.geoHash]
        val includedMissions = missions.filter { mission ->
            mission.triggerTime in (event.startTime until (event.endTime ?: toTime.coerceAtMost(curTime)))
        }
        SedentaryMissionEvent(
            event = event,
            place = place,
            missions = includedMissions
        )
    }
}
