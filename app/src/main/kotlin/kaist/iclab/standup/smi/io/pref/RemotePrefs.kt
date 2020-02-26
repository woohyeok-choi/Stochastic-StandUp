package kaist.iclab.standup.smi.io.pref

import kaist.iclab.standup.smi.common.FirebaseRemoteConfigHolder
import kaist.iclab.standup.smi.common.FirebaseRemoteConfigReadFloat
import kaist.iclab.standup.smi.common.FirebaseRemoteConfigReadLong
import java.util.concurrent.TimeUnit

object RemotePrefs : FirebaseRemoteConfigHolder() {
    val sedentaryThreshold: Long by FirebaseRemoteConfigReadLong(
        default = TimeUnit.MINUTES.toMillis(
            60
        )
    )
    val distanceThreshold: Float by FirebaseRemoteConfigReadFloat(default = 50.0F)
    val windowSize: Long by FirebaseRemoteConfigReadLong(default = TimeUnit.DAYS.toMillis(7))
    val minIncentive: Float by FirebaseRemoteConfigReadFloat(default = 100.0F)
    val maxIncentive: Float by FirebaseRemoteConfigReadFloat(default = 500.0F)
    val defaultIncentive: Float by FirebaseRemoteConfigReadFloat(default = 300.0F)
}