package kaist.iclab.standup.smi.pref

import kaist.iclab.standup.smi.BuildConfig
import kaist.iclab.standup.smi.common.FirebaseRemoteConfigBoolean
import kaist.iclab.standup.smi.common.FirebaseRemoteConfigHolder
import kaist.iclab.standup.smi.common.FirebaseRemoteConfigInt
import kaist.iclab.standup.smi.common.FirebaseRemoteConfigLong
import java.util.concurrent.TimeUnit

object RemotePrefs : FirebaseRemoteConfigHolder(BuildConfig.REMOTE_PREF_NAME) {
    /**
     * Minimum duration of staying event.
     * If stay duration is larger than it, the event is regarded as stay event which
     * will be used to calculate incentives.
     */
    var minTimeForStayEvent: Long by FirebaseRemoteConfigLong(default = TimeUnit.MINUTES.toMillis(30))

    /**
     * Minimum duration for mission trigger.
     * If stay duration is larger than it, the mission is triggered.
     */
    var minTimeForMissionTrigger: Long by FirebaseRemoteConfigLong(default = TimeUnit.MINUTES.toMillis(50))

    /**
     * Timeout of mission
     */
    var timeoutForMissionExpired: Long by FirebaseRemoteConfigLong(default = TimeUnit.MINUTES.toMillis(10))

    /**
     * Maximum daily budget
     */
    var maxDailyBudget: Int by FirebaseRemoteConfigInt(default = 1000)

    /**
     * Maximum number of setting do-not-disturb mode
     */
    var maxDoNotDisturb: Int by FirebaseRemoteConfigInt(default = 2)

    /**
     * From here, detailed setting for incentive calculations
     */
    var winSizeInMillis: Long by FirebaseRemoteConfigLong(default = TimeUnit.DAYS.toMillis(7))
    var winSizeInNumber: Int by FirebaseRemoteConfigInt(default = 500)
    var minIncentives: Int by FirebaseRemoteConfigInt(default = 100)
    var maxIncentives: Int by FirebaseRemoteConfigInt(default = 500)
    var defaultIncentives: Int by FirebaseRemoteConfigInt(default = 300)
    var unitIncentives: Int by FirebaseRemoteConfigInt(default = 20)
}