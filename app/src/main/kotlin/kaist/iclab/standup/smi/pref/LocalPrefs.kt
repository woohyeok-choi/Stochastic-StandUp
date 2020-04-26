package kaist.iclab.standup.smi.pref

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.ReadWriteBoolean
import github.agustarc.koap.delegator.ReadWriteInt
import github.agustarc.koap.delegator.ReadWriteLong
import github.agustarc.koap.delegator.ReadWriteString
import kaist.iclab.standup.smi.BuildConfig
import java.util.concurrent.TimeUnit

object LocalPrefs : PreferenceHolder(BuildConfig.PREF_NAME) {
    /**
     * Allow users to set belows
     */
    var activeStartTimeMs: Long by ReadWriteLong(default = TimeUnit.HOURS.toMillis(9))
    var activeEndTimeMs: Long by ReadWriteLong(default = TimeUnit.HOURS.toMillis(21))
    var doNotDisturbUntil: Long by ReadWriteLong(default = -1)
    var isMissionOn: Boolean by ReadWriteBoolean(default = true)

    /**
     * Internal use
     */
    var missionIdInProgress: String by ReadWriteString(default = "")
    var isMissionInProgress: Boolean by ReadWriteBoolean(default = false)
    var lastStillTime: Long by ReadWriteLong(default = -1L)
    var doNotDisturbLastTimeSettingMillis: Long by ReadWriteLong(default = -1L)
    var doNotDisturbCount: Int by ReadWriteInt(default = 0)
    var isSedentary: Boolean by ReadWriteBoolean(default = true)

    /**
     * Debug use
     */
    var incentiveMode: Int by ReadWriteInt(default = BuildConfig.INCENTIVE_MODE)
    var isGainIncentive: Boolean by ReadWriteBoolean(default = BuildConfig.IS_GAIN_INCENTIVE)
}
