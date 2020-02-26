package kaist.iclab.standup.smi.io.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents


object Events : Documents("events") {
    val type = string("type")
    val startTime = long("startTime")
    val endTime = long("endTime")
    val latitude = double("latitude")
    val longitude = double("longitude")
}

class Event: DocumentEntity() {
    var type: String? by Events.type
    var startTime: Long? by Events.startTime
    var endTime : Long? by Events.endTime
    var latitude : Double? by Events.latitude
    var longitude : Double? by Events.longitude

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if(type != other.type) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }

    override fun toString(): String = """
        ${javaClass.simpleName} [id=$id, type=$type, startTime=$startTime, endTime=$endTime, latitude=$latitude, longitude=$longitude]
    """.trimIndent()

    companion object : DocumentEntityClass<Event>(
        Events
    ) {
        const val TYPE_SEDENTARY = "SEDENTARY"
        const val TYPE_VEHICLE = "VEHICLE"
    }
}
