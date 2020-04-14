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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import kaist.iclab.standup.smi.base.BaseService
import kaist.iclab.standup.smi.common.Notifications
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.repository.MissionRepository
import kaist.iclab.standup.smi.repository.sumIncentives
import kotlinx.coroutines.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.koin.android.ext.android.inject
import java.lang.Exception
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class StandUpService : BaseService() {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val missionRepository: MissionRepository by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentTime = System.currentTimeMillis()

        if (intent?.action == ACTION_CANCEL_DO_NOT_DISTURB) {
            LocalPrefs.doNotDisturbUntil = 0
        }

        val cancelIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                applicationContext,
                REQUEST_CODE_CANCEL_DO_NOT_DISTURB,
                Intent(applicationContext, StandUpService::class.java).apply {
                    action = ACTION_CANCEL_DO_NOT_DISTURB
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                applicationContext,
                REQUEST_CODE_CANCEL_DO_NOT_DISTURB,
                Intent(applicationContext, StandUpService::class.java).apply {
                    action = ACTION_CANCEL_DO_NOT_DISTURB
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = Notifications.buildForegroundNotification(
            context = this,
            incentives = runBlocking(scope.coroutineContext) {
                getDailyIncentives(currentTime)
            },
            cancelIntent = cancelIntent,
            countDownUntil = LocalPrefs.doNotDisturbUntil
        )
        startForeground(Notifications.NOTIFICATION_ID_FOREGROUND, notification)

        if (LocalPrefs.doNotDisturbUntil > currentTime) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                LocalPrefs.doNotDisturbUntil,
                cancelIntent
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        stopForeground(true)
    }

    private suspend fun getDailyIncentives(timestamp: Long): Int {
        val dateTime = DateTime(timestamp, DateTimeZone.getDefault())
        val startTime = dateTime.withTimeAtStartOfDay().millis
        val endTime = dateTime.withTimeAtStartOfDay().plusDays(1).millis
        val missions = missionRepository.getCompletedMissions(
            fromTime = startTime,
            toTime = endTime
        )
        val incentive = missions.sumIncentives()
        return if (incentive >= 0) {
            incentive.coerceAtMost(RemotePrefs.maxDailyBudget)
        } else {
            (RemotePrefs.maxDailyBudget - abs(incentive)).coerceAtLeast(0)
        }
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
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    REQUEST_CODE_RESTART_SERVICE,
                    Intent(context, StandUpService::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getService(
                    context,
                    REQUEST_CODE_RESTART_SERVICE,
                    Intent(context, StandUpService::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
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
        const val ACTION_CANCEL_DO_NOT_DISTURB =
            "${BuildConfig.APPLICATION_ID}.ACTION_CANCEL_DO_NOT_DISTURB"
        const val REQUEST_CODE_CANCEL_DO_NOT_DISTURB = 0x10
        const val REQUEST_CODE_SMART_MANAGER_AVOID = 0x11
        const val REQUEST_CODE_RESTART_SERVICE = 0x12
        const val PACKAGE_NAME_SMART_MANAGER = "com.samsung.android.sm"

        fun startService(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, StandUpService::class.java))
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, StandUpService::class.java))
        }
    }
}