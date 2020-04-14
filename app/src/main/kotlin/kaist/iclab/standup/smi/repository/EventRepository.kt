package kaist.iclab.standup.smi.repository

import com.google.firebase.firestore.CollectionReference
import kaist.iclab.standup.smi.common.toGeoHash
import kaist.iclab.standup.smi.common.toISODateTime
import kaist.iclab.standup.smi.data.*
import java.util.*

class EventRepository(
    private val reference: () -> CollectionReference?
) {
    suspend fun createEvent(
        eventTime: Long,
        entered: Boolean,
        latitude: Double,
        longitude: Double
    ) : String? {
        val reference = reference.invoke() ?: return null

        return Event.create(reference, eventTime.toISODateTime()) {
            this.offsetMs = TimeZone.getDefault().rawOffset
            this.isEntered = entered
            this.timestamp = eventTime
            this.latitude = latitude
            this.longitude = longitude
            this.geoHash = (latitude to longitude).toGeoHash() ?: ""
        }
    }

    suspend fun getEvents(fromTime: Long, toTime: Long): List<Event> {
        val reference = reference.invoke() ?: return listOf()

        return Event.select(
            ref = reference,
            orderBy = Events.timestamp,
            isAscending = false
        ) {
            Events.timestamp greaterThanOrEqualTo fromTime
            Events.timestamp lessThan toTime
        }
    }
}