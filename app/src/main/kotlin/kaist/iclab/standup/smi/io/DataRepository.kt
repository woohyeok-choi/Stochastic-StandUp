package kaist.iclab.standup.smi.io

import com.google.firebase.firestore.DocumentReference
import kaist.iclab.standup.smi.io.data.Event
import kaist.iclab.standup.smi.io.data.Events
import org.joda.time.LocalDate
import org.joda.time.Period
import java.util.*
import kotlin.math.min

class DataRepository(private val ref: () -> DocumentReference?) {

    private fun toLocalDate(timestamp: Long): LocalDate = LocalDate.fromCalendarFields(
        GregorianCalendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = timestamp }
    )

    /**
     * Make any event that is not completed even if the day of starting is passed completed in the day
     * and create new event.
     * This is called only when "READING DATA", not writing them.
     */
    suspend fun fillEventsOfDifferentDays(
        reference: DocumentReference,
        curTimeMillis: Long,
        lastEventId: String
    ): String? {
        val latestEvent = Event.get(reference, lastEventId) ?: return lastEventId

        val startTimeMillis = latestEvent.startTime ?: return lastEventId
        val endTimeMillis = latestEvent.endTime

        val startDate = toLocalDate(startTimeMillis)
        val endDate = endTimeMillis?.let { toLocalDate(it) }
        val curDate = toLocalDate(curTimeMillis)

        /**
         * Case 1:
         * - endTime == null (not completed yet) && startTime != curTime (a day is passed)
         * --> endTime = endOf0thDay
         * --> For i in 1 to curDate; fill new events (startTime = startOfithDay, endTime = min(endOfithDay, curTime))
         *
         * Case 2:
         * - endTime != null (completed) && startTime != endTime (completed in different days)
         * --> endTime = endOf0thDay
         * --> For i in 1 to endTime; fill new events (startTime = startOfithDay, endTime = min(endOfithDay, endTime)
         * Other cases:
         * - endTime == null (not completed yet) && startTime == curTime: A day is not yet passed
         * - endTime != null (not completed yet) && startTime == endTime: Complete event in a same day.
         */

        val period = if (endDate == null && startDate != curDate) {
            Period(startDate, curDate)
        } else if (endDate != null && startDate != endDate) {
            Period(startDate, endDate)
        } else {
            null
        } ?: return lastEventId

        val days = period.toStandardDays().days

        Event.update(reference, lastEventId) {
            endTime = startDate.plusDays(1).toDate().time
        }

        return (1..days).mapNotNull { day ->
            Event.create(reference) {
                type = latestEvent.type
                startTime = startDate.plusDays(day).toDate().time
                endTime =
                    min(startDate.plusDays(day + 1).toDate().time, endTimeMillis ?: curTimeMillis)
                latitude = latestEvent.latitude
                longitude = latestEvent.longitude
            }
        }.firstOrNull()
    }

    suspend fun beginEvent(
        reference: DocumentReference,
        eventType: String,
        timestamp: Long,
        locLatitude: Double?,
        locLongitude: Double?
    ): String? = Event.create(reference) {
        type = eventType
        startTime = timestamp
        latitude = locLatitude
        longitude = locLongitude
    }

    suspend fun getLatestIncompleteEvent(reference: DocumentReference) = Event.select(reference) {
        Events.endTime equalTo null
        orderBy(Events.startTime)
        limit(1)
    }.firstOrNull()


    suspend fun endEvent(reference: DocumentReference, timestamp: Long) {
        val id = Event.select(reference) {
            Events.endTime equalTo null
            orderBy(Events.startTime)
            limit(1)
        }.firstOrNull()?.id ?: return

        Event.update(reference, id) {
            endTime = timestamp
        }
    }

    suspend fun findSedentaryEvents(startTime: Long, endTime: Long) = Event.select(ref.invoke()) {
        Events.startTime lessThan endTime
        Events.endTime greaterThanOrEqualTo startTime
    }
}