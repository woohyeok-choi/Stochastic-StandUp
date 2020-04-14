package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents

object LocationStats : Documents() {
    val name = string("name")
    val address = string("address")
    val geoHash = string("geoHash")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val gain = integer("gain")
    val loss = integer("loss")
    val numStayed = integer("numStayed")
    val numMission = integer("numMission")
    val numSuccess = integer("numSuccess")
    val approximateDuration = long("approximateDuration")
}

class LocationStat : DocumentEntity() {
    var name by LocationStats.name
    var address by LocationStats.address
    var geoHash by LocationStats.geoHash
    var latitude by LocationStats.latitude
    var longitude by LocationStats.longitude
    var gain by LocationStats.gain
    var loss by LocationStats.loss
    var numStayed by LocationStats.numStayed
    var numMission by LocationStats.numMission
    var numSuccess by LocationStats.numSuccess
    var approximateDuration by LocationStats.approximateDuration

    companion object : DocumentEntityClass<LocationStat>(LocationStats)
}

object AccumulatedStats: Documents() {
    val gain = integer("gain")
    val loss = integer("loss")
    val numStayed = integer("numStayed")
    val numMission = integer("numMission")
    val numSuccess = integer("numSuccess")
    val approximateDuration = long("approximateDuration")
}

class AccumulatedStat: DocumentEntity() {
    var gain by AccumulatedStats.gain
    var loss by AccumulatedStats.loss
    var numStayed by AccumulatedStats.numStayed
    var numMission by AccumulatedStats.numMission
    var numSuccess by AccumulatedStats.numSuccess
    var approximateDuration by AccumulatedStats.approximateDuration

    companion object : DocumentEntityClass<AccumulatedStat>(AccumulatedStats)
}