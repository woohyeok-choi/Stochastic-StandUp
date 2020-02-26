package kaist.iclab.standup.smi.io.pref

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.ReadWriteFloat
import github.agustarc.koap.delegator.ReadWriteLong
import kaist.iclab.standup.smi.BuildConfig
import java.util.concurrent.TimeUnit

object DebugPrefs : PreferenceHolder(BuildConfig.DEBUG_PREF_NAME) {
    var sedentaryThreshold: Long by ReadWriteLong(default = TimeUnit.MINUTES.toMillis(60))
    var distanceThreshold: Float by ReadWriteFloat(default = 50.0F)
    var windowSize: Long by ReadWriteLong(default = TimeUnit.DAYS.toMillis(7))
    var minIncentive: Float by ReadWriteFloat(default = 100.0F)
    var maxIncentive: Float by ReadWriteFloat(default = 500.0F)
    var defaultIncentive: Float by ReadWriteFloat(default = 300.0F)
}
