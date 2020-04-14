package kaist.iclab.standup.smi.pref

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.*
import kaist.iclab.standup.smi.BuildConfig
import java.util.concurrent.TimeUnit

object LocalPrefs : PreferenceHolder(BuildConfig.PREF_NAME) {
    /**
     * Allow users to set belows
     */
    var activeStartTimeMs: Long by ReadWriteLong(default = TimeUnit.HOURS.toMillis(9))
    var activeEndTimeMs: Long by ReadWriteLong(default = TimeUnit.HOURS.toMillis(21))
    var doNotDisturbUntil: Long by ReadWriteLong(default = -1)

    /**
     * Internal use
     */
    var firstUsageTime: Long by ReadWriteLong(default = System.currentTimeMillis())
    var missionIdInProgress: String by ReadWriteString(default = "")
    var isMissionInProgress: Boolean by ReadWriteBoolean(default = false)
    var doNotDisturbLastTimeSettingMillis: Long by ReadWriteLong(default = -1L)
    var doNotDisturbCount: Int by ReadWriteInt(default = 0)
}
