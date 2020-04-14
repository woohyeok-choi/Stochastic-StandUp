package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents

object LocationStats : Documents() {
    val name = string("name", "")
    val address = string("address", "")
    val incentive = long("incentive", 0)
    val numStayed = long("numStayed", 0)
    val numMission = long("numMission", 0)
    val numSuccess = long("numSuccess", 0)
    val approximateDuration = long("approximateDuration", 0)
    val lastVisitTime = long("lastVisitTime", -1)
}

class LocationStat : DocumentEntity() {
    var name by LocationStats.name
    var address by LocationStats.address
    var incentive by LocationStats.incentive
    var numStayed by LocationStats.numStayed
    var numMission by LocationStats.numMission
    var numSuccess by LocationStats.numSuccess
    var approximateDuration by LocationStats.approximateDuration
    var lastVisitTime by LocationStats.lastVisitTime

    companion object : DocumentEntityClass<LocationStat>(LocationStats)
}
