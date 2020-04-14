package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents

object Sessions : Documents("sessions") {
    val offsetMs = integer("offsetMs")
    val startTime = long("startTime")
    val deliveredTime = long("deliveredTime")
    val state = string("state")
    val reactionTime = long("reactionTime")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val geoHash = string("geoHash")
    val incentive = integer("incentive")
}

class Session : DocumentEntity() {
    var offsetMs : Int? by Sessions.offsetMs
    var latitude : Double? by Sessions.latitude
    var longitude: Double? by Sessions.longitude
    var geoHash: String? by Sessions.geoHash
    var incentive: Int? by Sessions.incentive
    var startTime : Long? by Sessions.startTime
    var deliveredTime : Long? by Sessions.deliveredTime
    var state: String? by Sessions.state
    var reactionTime : Long? by Sessions.reactionTime

    companion object : DocumentEntityClass<Session>(Sessions) {
        const val STATE_STAND_BY = "STAND_BY"
        const val STATE_ON_MISSION = "ON_MISSION"
        const val STATE_SUCCESS = "SUCCESS"
        const val STATE_CHEAT = "CHEAT"
        const val STATE_FAILURE = "FAILURE"
    }
}