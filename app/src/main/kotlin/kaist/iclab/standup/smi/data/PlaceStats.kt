package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents

object PlaceStats : Documents() {
    val name = string("name", "")
    val address = string("address", "")
    val incentive = long("incentive", 0)
    val numVisit = long("numVisit", 0)
    val numMission = long("numMission", 0)
    val numSuccess = long("numSuccess", 0)
    val lastVisitTime = long("lastVisitTime", -1)
}

class PlaceStat : DocumentEntity() {
    var name by PlaceStats.name
    var address by PlaceStats.address
    var incentive by PlaceStats.incentive
    var numVisit by PlaceStats.numVisit
    var numMission by PlaceStats.numMission
    var numSuccess by PlaceStats.numSuccess
    var lastVisitTime by PlaceStats.lastVisitTime

    companion object : DocumentEntityClass<PlaceStat>(PlaceStats)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PlaceStat

        if (id != other.id) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (incentive != other.incentive) return false
        if (numVisit != other.numVisit) return false
        if (numMission != other.numMission) return false
        if (numSuccess != other.numSuccess) return false
        if (lastVisitTime != other.lastVisitTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + incentive.hashCode()
        result = 31 * result + numVisit.hashCode()
        result = 31 * result + numMission.hashCode()
        result = 31 * result + numSuccess.hashCode()
        result = 31 * result + lastVisitTime.hashCode()
        return result
    }

    override fun toString(): String = StringBuilder(javaClass.simpleName)
        .append(" (")
        .append("id=$id, ")
        .append("name=$name, ")
        .append("address=$address, ")
        .append("incentive=$incentive, ")
        .append("numVisit=$numVisit, ")
        .append("numMission=$numMission, ")
        .append("numSuccess=$numSuccess, ")
        .append("lastVisitTime=$lastVisitTime")
        .append(")")
        .toString()
}
