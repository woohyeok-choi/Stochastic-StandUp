package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents
import kaist.iclab.standup.smi.pref.RemotePrefs

object OverallStats: Documents() {
    val numPlaces = long("numPlaces", 0)
    val incentive = long("incentive", 0)
    val numVisit = long("numVisit", 0)
    val numMission = long("numMission", 0)
    val numSuccess = long("numSuccess", 0)
    val approximateDuration = long("approximateDuration", 0)
}

class OverallStat: DocumentEntity() {
    var numPlaces by OverallStats.numPlaces
    var incentive by OverallStats.incentive
    var numVisit by OverallStats.numVisit
    var numMission by OverallStats.numMission
    var numSuccess by OverallStats.numSuccess
    var approximateDuration by OverallStats.approximateDuration

    companion object : DocumentEntityClass<OverallStat>(OverallStats)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OverallStat

        if (id != other.id) return false
        if (numPlaces != other.numPlaces) return false
        if (incentive != other.incentive) return false
        if (numVisit != other.numVisit) return false
        if (numMission != other.numMission) return false
        if (numSuccess != other.numSuccess) return false
        if (approximateDuration != other.approximateDuration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + numPlaces.hashCode()
        result = 31 * result + incentive.hashCode()
        result = 31 * result + numVisit.hashCode()
        result = 31 * result + numMission.hashCode()
        result = 31 * result + numSuccess.hashCode()
        result = 31 * result + approximateDuration.hashCode()
        return result
    }

    override fun toString(): String = StringBuilder(javaClass.simpleName)
        .append(" (")
        .append("id=$id, ")
        .append("numPlaces=$numPlaces, ")
        .append("incentive=$incentive, ")
        .append("numVisit=$numVisit, ")
        .append("numMission=$numMission, ")
        .append("numSuccess=$numSuccess, ")
        .append("approximateDuration=$approximateDuration")
        .append(")")
        .toString()
}