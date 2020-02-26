package kaist.iclab.standup.smi.io.data

import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Documents

object Incentives : Documents("incentives") {
    val historyId = string("historyId")
    val deliveredTime = long("deliveredTime")
    val reactionTime = long("reactionTime")
    val standUpTime = long("standUpTime")
    val amount = float("amount")
    val state = string("state")
}

class Incentive : DocumentEntity() {
    var historyId : String? by Incentives.historyId
    var deliveredTime : Long? by Incentives.deliveredTime
    var reactionTime : Long? by Incentives.reactionTime
    var standUpTime: Long? by Incentives.standUpTime
    var amount: Float? by Incentives.amount
    var state: String? by Incentives.state

    companion object : DocumentEntityClass<Incentive>(
        Incentives
    ) {
        const val STATE_ACCEPTED = "ACCEPTED"
        const val STATE_ADHERED = "ADHERED"
        const val STATE_REJECTED = "REJECTED"
        const val STATE_IGNORED = "IGNORED"
    }
}