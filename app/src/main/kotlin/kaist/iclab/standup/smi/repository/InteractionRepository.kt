package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.common.regularId
import kaist.iclab.standup.smi.pref.LocalPrefs
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class SettingRepository {
    private val accLossAtomic: AtomicInteger = AtomicInteger(LocalPrefs.accumulatedLoss)
    private val accDoNotDisturbAtomic: AtomicInteger = AtomicInteger(LocalPrefs.accumulatedDoNotDisturb)
    private val doNotDisturbUntilAtomic: AtomicLong = AtomicLong(LocalPrefs.doNotDisturbUntil)
    private val isMissionInProgressAtomic: AtomicBoolean = AtomicBoolean(LocalPrefs.isMissionInProgress)

    private var accumulatedLoss: Int
        get() = accLossAtomic.get()
        set(value) {
            LocalPrefs.accumulatedLoss = value
            accLossAtomic.set(value)
        }

    private var accumulatedDoNotDisturb: Int
        get() = accDoNotDisturbAtomic.get()
        set(value) {
            LocalPrefs.accumulatedDoNotDisturb = value
            accDoNotDisturbAtomic.set(value)
        }

    var doNotDisturbUntil: Long
        get() = doNotDisturbUntilAtomic.get()
        private set(value) {
            LocalPrefs.doNotDisturbUntil = value
            doNotDisturbUntilAtomic.set(value)
        }

    var isMissionInProgress: Boolean
        get() = isMissionInProgressAtomic.get()
        set(value) {
            LocalPrefs.isMissionInProgress = value
            isMissionInProgressAtomic.set(value)
        }

    private fun checkDayChange(timestamp: Long) {
        val oldId = LocalPrefs.budgetId
        val curId = regularId(timestamp)

        if (oldId != curId) {
            LocalPrefs.budgetId = oldId
            accumulatedLoss = 0
            accumulatedDoNotDisturb = 0
        }
    }

    fun getAccumulatedLoss(timestamp: Long) : Int {
        checkDayChange(timestamp)
        return accumulatedLoss
    }

    fun getAccumulatedDoNotDisturb(timestamp: Long) : Int {
        checkDayChange(timestamp)
        return accumulatedDoNotDisturb
    }

    fun incrementLoss(timestamp: Long, amount: Int) {
        checkDayChange(timestamp)
        accumulatedLoss += amount
    }

    fun startDoNotDisturbMode(timestamp: Long, until: Long) {
        checkDayChange(timestamp)
        accumulatedDoNotDisturb += 1
        doNotDisturbUntil = until
    }

    fun cancelDoNotDisturbMode(timestamp: Long) {
        checkDayChange(timestamp)
        doNotDisturbUntil = 0
    }

    fun isDoNotDisturbMode() = doNotDisturbUntil > 0
}