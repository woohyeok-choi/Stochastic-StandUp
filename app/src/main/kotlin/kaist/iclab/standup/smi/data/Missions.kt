package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents
import java.util.*

object Missions : Documents() {
    val offsetMs = integer("offsetMs", TimeZone.getDefault().rawOffset)
    val prepareTime = long("prepareTime", -1)
    val standByTime = long("standByTime", -1)
    val triggerTime = long("triggerTime", -1)
    val reactionTime = long("reactionTime", -1)
    val state = string("state", "")
    val latitude = double("latitude", Double.NaN)
    val longitude = double("longitude", Double.NaN)
    val geoHash = string("geoHash", "")
    val incentive = integer("incentive", 0)
}

class Mission : DocumentEntity() {
    var offsetMs by Missions.offsetMs
    var prepareTime by Missions.prepareTime
    var standByTime by Missions.standByTime
    var triggerTime by Missions.triggerTime
    var reactionTime by Missions.reactionTime
    var state by Missions.state
    var latitude  by Missions.latitude
    var longitude by Missions.longitude
    var geoHash by Missions.geoHash
    var incentive by Missions.incentive

    fun isSucceeded() = state.equals(STATE_SUCCESS, true)
    fun isFailed() = state.equals(STATE_FAILURE, true)
    fun isPrepared() = state.equals(STATE_PREPARED, true)
    fun isStandBy() = state.equals(STATE_STAND_BY, true)
    fun isTriggered() = state.equals(STATE_TRIGGERED, true)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Mission

        if (id != other.id) return false
        if (offsetMs != other.offsetMs) return false
        if (prepareTime != other.prepareTime) return false
        if (standByTime != other.standByTime) return false
        if (triggerTime != other.triggerTime) return false
        if (reactionTime != other.reactionTime) return false
        if (state != other.state) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (geoHash != other.geoHash) return false
        if (incentive != other.incentive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + offsetMs.hashCode()
        result = 31 * result + prepareTime.hashCode()
        result = 31 * result + standByTime.hashCode()
        result = 31 * result + triggerTime.hashCode()
        result = 31 * result + reactionTime.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + geoHash.hashCode()
        result = 31 * result + incentive.hashCode()
        return result
    }

    override fun toString(): String = StringBuilder(javaClass.simpleName)
        .append(" (")
        .append("id=$id, ")
        .append("offsetMs=$offsetMs, ")
        .append("prepareTime=$prepareTime, ")
        .append("standByTime=$standByTime, ")
        .append("deliveredTime=$triggerTime, ")
        .append("reactionTime=$reactionTime, ")
        .append("state=$state, ")
        .append("latitude=$latitude, ")
        .append("longitude=$longitude, ")
        .append("geoHash=$geoHash, ")
        .append("incentive=$incentive")
        .append(")")
        .toString()

    companion object : DocumentEntityClass<Mission>(Missions) {
        const val STATE_SUCCESS = "SUCCESS"
        const val STATE_FAILURE = "FAILURE"
        const val STATE_TRIGGERED = "TRIGGERED"
        const val STATE_STAND_BY = "STAND_BY"
        const val STATE_PREPARED = "PREPARED"
    }
}