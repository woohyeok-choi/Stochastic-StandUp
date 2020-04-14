package kaist.iclab.standup.smi.pref

import kaist.iclab.standup.smi.BuildConfig
import kaist.iclab.standup.smi.common.*
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
    var isStochasticMode: Boolean by FirebaseRemoteConfigBoolean(default = false)

    val maxProlongedSedentaryTime: Long by lazy { minTimeForMissionTrigger + timeoutForMissionExpired }

    /**
     * Belows are used only for UI only. Do not change.
     */
    var maxStayTimePerSession: Long by FirebaseRemoteConfigLong(default = TimeUnit.HOURS.toMillis(3))
    var maxDailyNumProlongedSedentariness: Int by FirebaseRemoteConfigInt(default = 6)
}