package kaist.iclab.standup.smi.io

import android.location.Location
import com.fonfon.kgeohash.GeoHash
import com.google.firebase.firestore.DocumentReference
import io.reactivex.internal.operators.maybe.MaybeIsEmpty
import kaist.iclab.standup.smi.data.*
import kaist.iclab.standup.smi.pref.LocalPrefs
import java.util.*

class EventRepository(
    private val factory: () -> DocumentReference?
) {
    suspend fun createEvent(
        eventTime: Long,
        entered: Boolean,
        location: Location? = null
    ) : String? {
        val reference = factory.invoke() ?: return null

        return Event.create(reference) {
            offsetMs = TimeZone.getDefault().rawOffset
            isEntered = entered
            timestamp = eventTime
            latitude = location?.latitude
            longitude = location?.longitude
            geoHash = location?.let { GeoHash(it, 8).toString() }
        }
    }

    private suspend fun getAllEvents(): List<Event> {
        val reference = factory.invoke() ?: return listOf()

        return Event.select(
            ref = reference,
            orderBy = Events.timestamp,
            isAscending = false
        )
    }

    private suspend fun getEvents(fromTime: Long, toTime: Long): List<Event> {
        val reference = factory.invoke() ?: return listOf()

        return Event.select(
            ref = reference,
            orderBy = Events.timestamp,
            isAscending = false
        ) {
            Events.timestamp greaterThanOrEqualTo fromTime
            Events.timestamp lessThan toTime
        }
    }

    suspend fun getSedentaryDurationEvents(fromTime: Long, toTime: Long): List<SedentaryDurationEvent> {
        val events = getEvents(fromTime, toTime)
        return extractDurationEvents(events, fromTime, toTime)
    }

    suspend fun getSedentaryDurationEvents(timeRanges: List<Pair<Long, Long>>): Map<Pair<Long, Long>, List<SedentaryDurationEvent>> {
        val fromTime = timeRanges.minBy { it.first }?.first ?: return mapOf()
        val toTime = timeRanges.maxBy { it.second }?.second ?: return mapOf()
        val events = getEvents(fromTime, toTime)
        return timeRanges.associateWith { (from, to) ->
            extractDurationEvents(events, from, to).toList()
        }
    }

    suspend fun getSedentaryDurationEvents(vararg timeRanges: Pair<Long, Long>): Map<Pair<Long, Long>, List<SedentaryDurationEvent>> {
        val fromTime = timeRanges.minBy { it.first }?.first ?: return mapOf()
        val toTime = timeRanges.maxBy { it.second }?.second ?: return mapOf()
        val events = getEvents(fromTime, toTime)
        return timeRanges.associate { (from, to) ->
            (from to to) to extractDurationEvents(events, from, to).toList()
        }
    }

    private fun extractDurationEvents(
        events: List<Event>,
        fromTime: Long,
        toTime: Long
    ): MutableList<SedentaryDurationEvent> {
        val subEvents = events.filter { event ->
            val timestamp = event.timestamp ?: return@filter false
            timestamp in (fromTime until toTime)
        }.sortedBy { it.timestamp }

        return subEvents.foldIndexed(mutableListOf()) { index, acc, event ->
            val timestamp = event.timestamp ?: return@foldIndexed acc
            if (index == 0) {
                if (event.isEntered == false) {
                    val duration = timestamp - fromTime
                    acc.add(
                        SedentaryDurationEvent(
                            timestamp = fromTime,
                            duration = duration,
                            latitude = event.latitude,
                            longitude = event.longitude
                        )
                    )
                }
            } else if (index == subEvents.lastIndex) {
                if (event.isEntered == true) {
                    val duration = toTime - timestamp
                    acc.add(
                        SedentaryDurationEvent(
                            timestamp = timestamp,
                            duration = duration,
                            latitude = event.latitude,
                            longitude = event.longitude
                        )
                    )
                }
            }

            val nextEvent = subEvents.getOrNull(index + 1)
            if (event.isEntered == true && nextEvent?.isEntered == false) {
                nextEvent.timestamp?.let { it - timestamp }?.also { duration ->
                    acc.add(
                        SedentaryDurationEvent(
                            timestamp = timestamp,
                            duration = duration,
                            latitude = event.latitude,
                            longitude = event.longitude
                        )
                    )
                }
            }
            return@foldIndexed acc
        }
    }


}