package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents
import org.joda.time.DateTime
import org.joda.time.DateTimeZone


object SedentaryEvents : Documents("events") {
    val offsetMs = integer("offsetMs")
    val timestamp = long("timestamp")
    val isEntered = boolean("isEntered")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val activeStartTime = long("activeStartTime")
    val activeEndTime = long("activeEndTime")
}

class SedentaryEvent : DocumentEntity() {
    var offsetMs: Int? by SedentaryEvents.offsetMs
    var timestamp: Long? by SedentaryEvents.timestamp
    var isEntered: Boolean? by SedentaryEvents.isEntered
    var latitude: Double? by SedentaryEvents.latitude
    var longitude: Double? by SedentaryEvents.longitude
    var activeStartTime: Long? by SedentaryEvents.activeStartTime
    var activeEndTime: Long? by SedentaryEvents.activeEndTime

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SedentaryEvent
        if (offsetMs != other.offsetMs) return false
        if (timestamp != other.timestamp) return false
        if (isEntered != other.isEntered) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (activeStartTime != other.activeStartTime) return false
        if (activeEndTime != other.activeEndTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offsetMs.hashCode()
        result = 31 * result + isEntered.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + activeStartTime.hashCode()
        result = 31 * result + activeEndTime.hashCode()
        return result
    }

    override fun toString(): String =
        "${javaClass.simpleName} [id=$id, timestamp=$timestamp, isEntered=$isEntered, latitude=$latitude, longitude=$longitude]"

    fun toHumanReadableString(timeZone: DateTimeZone? = null): String {
        val dateTimeZone = timeZone ?: DateTimeZone.forOffsetMillis(offsetMs ?: 0)

        return "${javaClass.simpleName} [id=$id, startTime=${timestamp?.let {
            DateTime(
                it,
                dateTimeZone
            )
        }}, isEntered=$isEntered, latitude=$latitude, longitude=$longitude]"
    }

    companion object : DocumentEntityClass<SedentaryEvent>(
        SedentaryEvents
    )
}

data class SedentaryDurationEvent(
    val timestamp: Long,
    val duration: Long,
    val latitude: Double?,
    val longitude: Double?
)
