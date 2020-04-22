package kaist.iclab.standup.smi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import kaist.iclab.standup.smi.base.BaseService
import kaist.iclab.standup.smi.common.AppLog
import kaist.iclab.standup.smi.common.Notifications
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.tracker.ActivityTracker
import kaist.iclab.standup.smi.tracker.LocationTracker
import kaist.iclab.standup.smi.tracker.StepCountTracker
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.random.Random

class StandUpService : BaseService() {
    private val locationTracker: LocationTracker by inject()
    private val stepCountTracker: StepCountTracker by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        locationTracker.startTracking()
        stepCountTracker.startTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        Notifications.notifyForegroundStatus(
            context = this,
            lastStillTime = LocalPrefs.lastStillTime,
            doNotDisturbUntil = LocalPrefs.doNotDisturbUntil,
            cancelIntent = StandUpIntentService.intentForCancelDoNotDisturb(this),
            callFromForegroundService = true
        )

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        locationTracker.stopTracking()
        stepCountTracker.stopTracking()

        stopForeground(true)
    }

    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action?.toLowerCase(Locale.getDefault()) ?: return
            val filters = listOf(
                "android.intent.action.QUICKBOOT_POWERON",
                Intent.ACTION_BOOT_COMPLETED
            ).map { it.toLowerCase(Locale.getDefault()) }

            if (action !in filters) return
            if (context == null) return

            handleSmartManager(context)
            handleRestartService(context)
        }

        private fun handleSmartManager(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val currentTime = System.currentTimeMillis()

                context.packageManager.getPackageInfo(
                    PACKAGE_NAME_SMART_MANAGER,
                    PackageManager.GET_META_DATA
                ) ?: return

                val intent = PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_SMART_MANAGER_AVOID,
                    Intent(context, AvoidSmartManagerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    currentTime + Random.nextLong(1000, 5000),
                    intent
                )
            } catch (e: Exception) {

            }
        }

        private fun handleRestartService(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val currentTime = System.currentTimeMillis()
            val intent = getPendingIntent(
                context = context,
                code = REQUEST_CODE_RESTART_SERVICE
            )

            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                currentTime + Random.nextLong(5000, 7500),
                intent
            )
        }
    }

    class AvoidSmartManagerActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            overridePendingTransition(0, 0)
            finish()
        }
    }

    companion object {
        const val REQUEST_CODE_SMART_MANAGER_AVOID = 0x11
        const val REQUEST_CODE_RESTART_SERVICE = 0x12
        const val PACKAGE_NAME_SMART_MANAGER = "com.samsung.android.sm"

        fun startService(context: Context) {
            AppLog.d(StandUpService::class.java, "startService")
            ContextCompat.startForegroundService(context, Intent(context, StandUpService::class.java))
        }

        fun stopService(context: Context) {
            AppLog.d(StandUpService::class.java, "stopService")
            context.stopService(Intent(context, StandUpService::class.java))
        }

        private fun getPendingIntent(
            context: Context,
            code: Int,
            action: String? = null,
            flags: Int = PendingIntent.FLAG_UPDATE_CURRENT
        ): PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                code,
                Intent(context, StandUpService::class.java).apply {
                    if (!action.isNullOrBlank()) setAction(action)
                },
                flags
            )
        } else {
            PendingIntent.getService(
                context,
                REQUEST_CODE_RESTART_SERVICE,
                Intent(context, StandUpService::class.java).apply {
                    if (!action.isNullOrBlank()) setAction(action)
                },
                flags
            )
        }
    }
}