package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents
import java.util.*


object Events : Documents() {
    val offsetMs = integer("offsetMs", TimeZone.getDefault().rawOffset)
    val timestamp = long("timestamp", -1)
    val isEntered = boolean("isEntered", false)
    val latitude = double("latitude", Double.NaN)
    val longitude = double("longitude", Double.NaN)
    val geoHash = string("geoHash", "")
}

class Event : DocumentEntity() {
    var offsetMs by Events.offsetMs
    var timestamp by Events.timestamp
    var isEntered by Events.isEntered
    var latitude by Events.latitude
    var longitude by Events.longitude
    var geoHash by Events.geoHash

    companion object : DocumentEntityClass<Event>(Events)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Event

        if (id != other.id) return false
        if (offsetMs != other.offsetMs) return false
        if (timestamp != other.timestamp) return false
        if (isEntered != other.isEntered) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (geoHash != other.geoHash) return false


        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + offsetMs.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isEntered.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + geoHash.hashCode()


        return result
    }

    override fun toString(): String = StringBuilder(javaClass.simpleName)
        .append(" (")
        .append("id=$id, ")
        .append("offsetMs=$offsetMs, ")
        .append("timestamp=$timestamp, ")
        .append("isEntered=$isEntered, ")
        .append("latitude=$latitude, ")
        .append("longitude=$longitude, ")
        .append("geoHash=$geoHash")
        .append(")")
        .toString()
}

