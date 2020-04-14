package kaist.iclab.standup.smi.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents
import java.util.*

object Interactions : Documents() {
    val offsetMs = integer("offsetMs", TimeZone.getDefault().rawOffset)
    val timestamp = long("timestamp", -1)
    val type = string("type", "")
    val extras = string("extras", "")
}

class Interaction : DocumentEntity() {
    var offsetMs by Interactions.offsetMs
    var timestamp by Interactions.timestamp
    var type by Interactions.type
    var extras by Interactions.extras

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Interaction

        if (id != other.id) return false
        if (offsetMs != other.offsetMs) return false
        if (timestamp != other.timestamp) return false
        if (type != other.type) return false
        if (extras != other.extras) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + offsetMs.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + extras.hashCode()
        return result
    }

    override fun toString(): String = StringBuilder(javaClass.simpleName)
        .append(" (")
        .append("id=$id, ")
        .append("offsetMs=$offsetMs, ")
        .append("timestamp=$timestamp, ")
        .append("type=$type, ")
        .append("extras=$extras")
        .append(")")
        .toString()

    companion object: DocumentEntityClass<Interaction>(Interactions) {
        const val TYPE_DO_NOT_DISTURB = "TYPE_DO_NOT_DISTURB"
        const val TYPE_CANCEL_DO_NOT_DISTURB = "TYPE_CANCEL_DO_NOT_DISTURB"
        const val TYPE_OPEN_APP = "TYPE_OPEN_APP"
        const val TYPE_CLOSE_APP = "TYPE_CLOSE_APP"
        const val TYPE_CHANGE_SETTING = "TYPE_CHANGE_SETTING"
    }
}
