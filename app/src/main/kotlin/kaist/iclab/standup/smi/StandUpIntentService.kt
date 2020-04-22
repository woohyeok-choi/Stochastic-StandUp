package kaist.iclab.standup.smi

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import kaist.iclab.standup.smi.common.*
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.tracker.LocationTracker
import kaist.iclab.standup.smi.tracker.StepCountTracker
import kotlinx.coroutines.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class StandUpIntentService : IntentService(StandUpIntentService::class.java.simpleName), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    private val locationTracker: LocationTracker by inject()
    private val stepCountTracker: StepCountTracker by inject()
    private val standUpRepository: StandUpMissionHandler by inject()

    override fun onHandleIntent(intent: Intent?) = runBlocking(coroutineContext) {
        try {
            val timestamp = System.currentTimeMillis()
            val state = intent?.getStringExtra(EXTRA_MISSION_STATE) ?: ""
            var (latitude, longitude) = intent?.getDoubleExtra(EXTRA_LATITUDE, Double.NaN) to intent?.getDoubleExtra(
                EXTRA_LONGITUDE, Double.NaN)
            if (latitude == null || longitude == null || latitude.isNaN() || longitude.isNaN()) {
                val location = locationTracker.getLastLocation()
                latitude = location?.latitude ?: Double.NaN
                longitude = location?.longitude ?: Double.NaN
            }

            when (intent?.action) {
                ACTION_ACTIVITY_UPDATE -> {
                    val (isSedentary, eventTime) = stepCountTracker.extract(intent)
                    if (isSedentary) {
                        handleEnterIntoStill(
                            timestamp = eventTime,
                            latitude = latitude,
                            longitude = longitude
                        )
                    } else {
                        handleExitFromStill(
                            timestamp = eventTime,
                            latitude = latitude,
                            longitude = longitude
                        )
                    }
                }
                ACTION_MOCK_ENTER_INTO_STILL -> {
                    handleEnterIntoStill(
                        timestamp = timestamp,
                        latitude = latitude,
                        longitude = longitude
                    )
                }
                ACTION_MOCK_EXIT_FROM_STILL -> {
                    handleExitFromStill(
                        timestamp = timestamp,
                        latitude = latitude,
                        longitude = longitude
                    )
                }
                ACTION_MISSION -> when (state) {
                    STATE_PREPARE ->
                        handlePrepareMission(
                            timestamp = timestamp,
                            latitude = latitude,
                            longitude = longitude
                        )
                    STATE_STAND_BY ->
                        handleStandByMission(
                            timestamp = timestamp,
                            latitude = latitude,
                            longitude = longitude
                        )

                    STATE_TRIGGER ->
                        handleTriggerMission(
                            timestamp = timestamp,
                            latitude = latitude,
                            longitude = longitude
                        )

                    STATE_EXPIRED -> {
                        handleCompleteMission(
                            timestamp = timestamp,
                            latitude = latitude,
                            longitude = longitude,
                            isSucceeded = false
                        )
                    }
                }
                ACTION_SET_DO_NOT_DISTURB -> {
                    handleSetDoNotDisturb(timestamp)
                }
                ACTION_CANCEL_DO_NOT_DISTURB -> {
                    handleCancelDoNotDisturb()
                }
            }
        } catch (e: Exception) {
            AppLog.ee(clazz, "onHandleIntent()", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private suspend fun handleEnterIntoStill(timestamp: Long, latitude: Double, longitude: Double) {
        Log.d(javaClass.simpleName, "handleEnterIntoStill(timestamp = $timestamp, latitude = $latitude, longitude = $longitude)")

        if (latitude.isNaN() || longitude.isNaN()) {
            scheduleAlarm(
                triggerAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10),
                intent = intentForActivity(applicationContext, stepCountTracker.fillExtra(Bundle(), true, timestamp))
            )
            return
        }

        tryCatch {
            standUpRepository.enterIntoStill(
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude
            )
        }

        handlePrepareMission(
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude
        )

        if (LocalPrefs.lastStillTime < 0) {
            LocalPrefs.lastStillTime = timestamp
        }

        Notifications.notifyForegroundStatus(
            context = applicationContext,
            lastStillTime = LocalPrefs.lastStillTime,
            doNotDisturbUntil = LocalPrefs.doNotDisturbUntil,
            cancelIntent = intentForCancelDoNotDisturb(this),
            callFromForegroundService = false
        )
    }

    private suspend fun handleExitFromStill(timestamp: Long, latitude: Double, longitude: Double) {
        Log.d(javaClass.simpleName, "handleExitFromStill(timestamp = $timestamp, latitude = $latitude, longitude = $longitude)")

        if (latitude.isNaN() || longitude.isNaN()) {
            scheduleAlarm(
                triggerAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10),
                intent = intentForActivity(applicationContext, stepCountTracker.fillExtra(Bundle(), false, timestamp))
            )
            return
        }

        tryCatch {
            standUpRepository.exitFromStill(
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude
            )
        }

        handleCompleteMission(
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            isSucceeded = true
        )

        val intent = getPendingIntent(
            context = applicationContext,
            code = REQUEST_CODE_MISSION,
            action = ACTION_MISSION
        )

        cancelAlarm(
            intent = intent
        )

        LocalPrefs.lastStillTime = -1

        Notifications.notifyForegroundStatus(
            context = applicationContext,
            lastStillTime = LocalPrefs.lastStillTime,
            doNotDisturbUntil = LocalPrefs.doNotDisturbUntil,
            cancelIntent = intentForCancelDoNotDisturb(this),
            callFromForegroundService = false
        )
    }

    private fun handleSetDoNotDisturb(timestamp: Long) {
        if (LocalPrefs.doNotDisturbUntil > timestamp) {
            scheduleAlarm(
                triggerAt = LocalPrefs.doNotDisturbUntil,
                intent = intentForCancelDoNotDisturb(applicationContext)
            )
            Notifications.notifyForegroundStatus(
                context = applicationContext,
                lastStillTime = LocalPrefs.lastStillTime,
                doNotDisturbUntil = LocalPrefs.doNotDisturbUntil,
                cancelIntent = intentForCancelDoNotDisturb(this),
                callFromForegroundService = false
            )
        }
    }

    private fun handleCancelDoNotDisturb() {
        LocalPrefs.doNotDisturbUntil = -1

        Notifications.notifyForegroundStatus(
            context = applicationContext,
            lastStillTime = LocalPrefs.lastStillTime,
            doNotDisturbUntil = LocalPrefs.doNotDisturbUntil,
            cancelIntent = intentForCancelDoNotDisturb(this),
            callFromForegroundService = false
        )
    }

    private suspend fun handlePrepareMission(timestamp: Long, latitude: Double, longitude: Double) {
        val id = tryCatch {
            standUpRepository.prepareMission(
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude
            )
        }
        LocalPrefs.missionIdInProgress = id ?: ""

        Log.d(javaClass.simpleName, "handlePrepareMission(timestamp = $timestamp, latitude = $latitude, longitude = $longitude): id = $id")

        val intent = getPendingIntent(
            context = applicationContext,
            code = REQUEST_CODE_MISSION,
            action = ACTION_MISSION,
            extras = bundleOf(
                EXTRA_MISSION_STATE to STATE_STAND_BY,
                EXTRA_TIMESTAMP to timestamp,
                EXTRA_LATITUDE to latitude,
                EXTRA_LONGITUDE to longitude
            )
        )

        scheduleAlarm(
            triggerAt = timestamp + RemotePrefs.minTimeForStayEvent,
            intent = intent
        )
    }

    private suspend fun handleStandByMission(timestamp: Long, latitude: Double, longitude: Double) {
        val id = LocalPrefs.missionIdInProgress
        Log.d(javaClass.simpleName, "handleStandByMission(timestamp = $timestamp, latitude = $latitude, longitude = $longitude): id = $id")

        if (!id.isBlank()) {
            tryCatch {
                standUpRepository.standByMission(
                    timestamp = timestamp,
                    latitude = latitude,
                    longitude = longitude,
                    id = id
                )
            }
        }

        val intent = getPendingIntent(
            context = applicationContext,
            code = REQUEST_CODE_MISSION,
            action = ACTION_MISSION,
            extras = bundleOf(
                EXTRA_MISSION_STATE to STATE_TRIGGER,
                EXTRA_LATITUDE to latitude,
                EXTRA_LONGITUDE to longitude
            )
        )

        scheduleAlarm(
            triggerAt = timestamp + RemotePrefs.minTimeForMissionTrigger - RemotePrefs.minTimeForStayEvent,
            intent = intent
        )
    }

    private suspend fun handleTriggerMission(timestamp: Long, latitude: Double, longitude: Double) {
        val id = LocalPrefs.missionIdInProgress

        Log.d(javaClass.simpleName, "handleTriggerMission(timestamp = $timestamp, latitude = $latitude, longitude = $longitude): id = $id")

        if (!id.isBlank()) {
            val mission = tryCatch {
                standUpRepository.startMission(
                    timestamp = timestamp,
                    latitude = latitude,
                    longitude = longitude,
                    id = id
                )
            }

            if (checkMissionAvailable(timestamp)) {
                LocalPrefs.isMissionInProgress = true

                Notifications.notifyMissionStart(
                    context = applicationContext,
                    incentives = mission?.incentive ?: 0,
                    durationMinutes = TimeUnit.MILLISECONDS.toMinutes(RemotePrefs.minTimeForMissionTrigger),
                    countDownUntil = timestamp + RemotePrefs.timeoutForMissionExpired
                )
            }
        }

        val intent = getPendingIntent(
            context = applicationContext,
            code = REQUEST_CODE_MISSION,
            action = ACTION_MISSION,
            extras = bundleOf(
                EXTRA_MISSION_STATE to STATE_EXPIRED,
                EXTRA_LATITUDE to latitude,
                EXTRA_LONGITUDE to longitude
            )
        )

        scheduleAlarm(
            triggerAt = timestamp + RemotePrefs.timeoutForMissionExpired,
            intent = intent
        )
    }

    private suspend fun handleCompleteMission(timestamp: Long, latitude: Double, longitude: Double, isSucceeded: Boolean) {
        val id = LocalPrefs.missionIdInProgress
        val isMissionInProgress = LocalPrefs.isMissionInProgress

        LocalPrefs.isMissionInProgress = false
        LocalPrefs.missionIdInProgress = ""

        Log.d(javaClass.simpleName, "handleCompleteMission(timestamp = $timestamp, latitude = $latitude, longitude = $longitude, isSucceeded = $isSucceeded): id = $id")

        if (!id.isBlank()) {
            val mission = tryCatch {
                standUpRepository.completeMission(
                    timestamp = timestamp,
                    latitude = latitude,
                    longitude = longitude,
                    id = id,
                    isSucceeded = if (isMissionInProgress) isSucceeded else null
                )
            }

            if (!isSucceeded) {
                handlePrepareMission(
                    timestamp = timestamp,
                    latitude = latitude,
                    longitude = longitude
                )
            }

            if (isMissionInProgress) {
                Notifications.notifyMissionResult(
                    context = applicationContext,
                    incentives = mission?.incentive ?: 0,
                    isSucceeded = isSucceeded
                )
            }
        }
    }

    private fun checkMissionAvailable(timestamp: Long): Boolean {
        val isOffDoNotDisturbMode = LocalPrefs.doNotDisturbUntil < timestamp
        val isInActiveTime = DateTime(
            timestamp,
            DateTimeZone.getDefault()
        ).millisOfDay in (LocalPrefs.activeStartTimeMs..LocalPrefs.activeEndTimeMs)
        val isMissionOn = LocalPrefs.isMissionOn
        return isOffDoNotDisturbMode && isInActiveTime && isMissionOn
    }

    companion object {
        private const val ACTION_MISSION =
            "${BuildConfig.APPLICATION_ID}.ACTION_MISSION"

        private const val ACTION_LOCATION_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_LOCATION_UPDATE"

        private const val ACTION_ACTIVITY_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_ACTIVITY_UPDATE"

        private const val ACTION_MOCK_ENTER_INTO_STILL =
            "${BuildConfig.APPLICATION_ID}.MOCK_ENTER_INTO_STILL"

        private const val ACTION_MOCK_EXIT_FROM_STILL =
            "${BuildConfig.APPLICATION_ID}.ACTION_MOCK_EXIT_FROM_STILL"

        private const val ACTION_SET_DO_NOT_DISTURB =
            "${BuildConfig.APPLICATION_ID}.ACTION_SET_DO_NOT_DISTURB"

        private const val ACTION_CANCEL_DO_NOT_DISTURB =
            "${BuildConfig.APPLICATION_ID}.ACTION_CANCEL_DO_NOT_DISTURB"

        private const val REQUEST_CODE_MISSION = 0x01
        private const val REQUEST_CODE_LOCATION_UPDATE = 0x02
        private const val REQUEST_CODE_ACTIVITY_UPDATE = 0x03
        private const val REQUEST_CODE_CANCEL_DO_NOT_DISTURB = 0x05

        private const val EXTRA_MISSION_STATE = "${BuildConfig.APPLICATION_ID}.EXTRA_MISSION_STATE"
        private const val EXTRA_TIMESTAMP = "${BuildConfig.APPLICATION_ID}.EXTRA_TIMESTAMP"
        private const val EXTRA_LATITUDE = "${BuildConfig.APPLICATION_ID}.EXTRA_LATITUDE"
        private const val EXTRA_LONGITUDE = "${BuildConfig.APPLICATION_ID}.EXTRA_LONGITUDE"

        private const val STATE_PREPARE = "STATE_PREPARE"
        private const val STATE_STAND_BY = "STATE_STAND_BY"
        private const val STATE_TRIGGER = "STATE_START"
        private const val STATE_EXPIRED = "STATE_RESULT"

        private val clazz = StandUpIntentService::class.java

        private fun getPendingIntent(
            context: Context,
            code: Int,
            action: String,
            flag: Int = PendingIntent.FLAG_CANCEL_CURRENT,
            extras: Bundle? = null
        ): PendingIntent =
            PendingIntent.getService(
                context,
                code,
                Intent(context, clazz).setAction(action).replaceExtras(extras),
                flag
            )

        fun intentForLocation(context: Context) =
            getPendingIntent(
                context,
                REQUEST_CODE_LOCATION_UPDATE,
                ACTION_LOCATION_UPDATE,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        fun intentForActivity(context: Context, extras: Bundle? = null) =
            getPendingIntent(
                context,
                REQUEST_CODE_ACTIVITY_UPDATE,
                ACTION_ACTIVITY_UPDATE,
                PendingIntent.FLAG_UPDATE_CURRENT,
                extras
            )

        fun intentForCancelDoNotDisturb(context: Context) = getPendingIntent(
            context = context,
            code = REQUEST_CODE_CANCEL_DO_NOT_DISTURB,
            action = ACTION_CANCEL_DO_NOT_DISTURB,
            flag = PendingIntent.FLAG_UPDATE_CURRENT
        )

        fun sendEnterIntoStill(context: Context) {
            Intent(context, clazz).apply {
                action = ACTION_MOCK_ENTER_INTO_STILL
            }.let { intent ->
                context.startService(intent)
            }
        }

        fun sendExitFromStill(context: Context) {
            Intent(context, clazz).apply {
                action = ACTION_MOCK_EXIT_FROM_STILL
            }.let { intent ->
                context.startService(intent)
            }
        }

        fun doNotDisturb(context: Context) {
            Intent(context, clazz).apply {
                action = ACTION_SET_DO_NOT_DISTURB
            }.let { intent ->
                context.startService(intent)
            }
        }
    }
}