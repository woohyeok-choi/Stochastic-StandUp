package kaist.iclab.standup.smi.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.annotation.AnyRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kaist.iclab.standup.smi.BuildConfig
import kaist.iclab.standup.smi.R

object Notifications {
    private const val CHANNEL_ID_FOREGROUND = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_FOREGROUND"
    private const val CHANNEL_ID_MISSION_START = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_MISSION_START"
    private const val CHANNEL_ID_MISSION_SUCCESS = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_MISSION_SUCCESS"
    private const val CHANNEL_ID_MISSION_FAILURE = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_MISSION_FAILURE"

    const val NOTIFICATION_ID_FOREGROUND = 0x01
    const val NOTIFICATION_ID_MISSION = 0x01

    data class ChannelSetting(
        @StringRes val nameRes: Int,
        val category: String,
        val priority: Int,
        val visibility: Int,
        val importance: Int,
        @AnyRes val sound: Int? = null,
        val vibration: LongArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ChannelSetting

            if (nameRes != other.nameRes) return false
            if (category != other.category) return false
            if (priority != other.priority) return false
            if (visibility != other.visibility) return false
            if (importance != other.importance) return false
            if (sound != other.sound) return false
            if (vibration != null) {
                if (other.vibration == null) return false
                if (!vibration.contentEquals(other.vibration)) return false
            } else if (other.vibration != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = nameRes.hashCode()
            result = 31 * result + category.hashCode()
            result = 31 * result + priority
            result = 31 * result + visibility
            result = 31 * result + importance
            result = 31 * result + (sound?.hashCode() ?: 0)
            result = 31 * result + (vibration?.contentHashCode() ?: 0)
            return result
        }
    }

    private val settings = mapOf(
        CHANNEL_ID_FOREGROUND to ChannelSetting(
            nameRes = R.string.ntf_channel_name_foreground,
            category = NotificationCompat.CATEGORY_STATUS,
            priority = NotificationCompat.PRIORITY_MIN,
            visibility = NotificationCompat.VISIBILITY_SECRET,
            importance = NotificationManagerCompat.IMPORTANCE_MIN
        ),
        CHANNEL_ID_MISSION_START to ChannelSetting(
            nameRes = R.string.ntf_channel_name_mission_start,
            category = NotificationCompat.CATEGORY_EVENT,
            priority = NotificationCompat.PRIORITY_HIGH,
            visibility = NotificationCompat.VISIBILITY_PUBLIC,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            sound = R.raw.mission_start,
            vibration = longArrayOf(0, 300, 50, 750, 1000, 300, 50, 750, 1000, 300, 50, 750)
        ),
        CHANNEL_ID_MISSION_SUCCESS to ChannelSetting(
            nameRes = R.string.ntf_channel_name_mission_success,
            category = NotificationCompat.CATEGORY_EVENT,
            priority = NotificationCompat.PRIORITY_HIGH,
            visibility = NotificationCompat.VISIBILITY_PUBLIC,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            sound = R.raw.mission_success,
            vibration = longArrayOf(0, 300, 50, 300, 50, 1000)
        ),
        CHANNEL_ID_MISSION_FAILURE to ChannelSetting(
            nameRes = R.string.ntf_channel_name_mission_failure,
            category = NotificationCompat.CATEGORY_EVENT,
            priority = NotificationCompat.PRIORITY_HIGH,
            visibility = NotificationCompat.VISIBILITY_PUBLIC,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            sound = R.raw.mission_failure,
            vibration = longArrayOf(0, 1000, 50, 300, 50, 300)
        )
    )

    fun bind(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buildChannels(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildChannels(context: Context) {
        val channels = settings.map { (id, setting) ->
            NotificationChannel(id, context.getString(setting.nameRes), setting.importance).apply {
                lockscreenVisibility = setting.visibility
                if (setting.vibration != null) vibrationPattern = setting.vibration
                if (setting.sound != null) {
                    setSound(
                        context.getResourceUri(setting.sound),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
            }
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(channels)
    }

    private fun getNotificationBuilder(context: Context, id: String): NotificationCompat.Builder? {
        val setting = settings[id] ?: return null
        return NotificationCompat.Builder(context, id).apply {
            setVisibility(setting.visibility)
            priority = setting.priority
            setCategory(setting.category)
            if (setting.sound != null) {
                setSound(context.getResourceUri(setting.sound), AudioManager.STREAM_NOTIFICATION)
            }
            if (setting.vibration != null) {
                setVibrate(setting.vibration)
            }
        }
    }

    fun buildForegroundNotification(
        context: Context,
        cancelIntent: PendingIntent,
        countDownUntil: Long
    ): Notification {
        val currentTime = System.currentTimeMillis()
        val builder =
            getNotificationBuilder(context, CHANNEL_ID_FOREGROUND) ?: NotificationCompat.Builder(
                context,
                CHANNEL_ID_FOREGROUND
            )

        return builder.apply {
            setOngoing(true)
            setLocalOnly(true)
            setAutoCancel(false)
            setContentTitle(context.getString(R.string.ntf_foreground_title))
            setSmallIcon(R.drawable.mission_24)
            color = context.getColor(R.color.blue)
            if (countDownUntil > currentTime) {
                addAction(
                    R.drawable.baseline_close_24,
                    context.getString(R.string.ntf_foreground_action_cancel),
                    cancelIntent
                )
                setContentText(
                    context.getString(
                        R.string.ntf_foreground_text_do_not_disturb,
                        DateUtils.formatDateTime(context, countDownUntil, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
                    )
                )
            } else {
                setContentText(context.getString(R.string.ntf_foreground_text_normal))
            }
        }.build()
    }

    fun buildMissionStartNotification(
        context: Context,
        incentives: Int,
        durationMinutes: Long,
        countDownUntil: Long
    ): Notification {
        val builder =
            getNotificationBuilder(context, CHANNEL_ID_MISSION_START) ?: NotificationCompat.Builder(
                context,
                CHANNEL_ID_MISSION_START
            )
        return builder.apply {
            extras = Bundle()
            setOngoing(true)
            setLocalOnly(false)
            setAutoCancel(false)
            setWhen(countDownUntil)
            setUsesChronometer(true)
            setChronometerCountDown(true)
            setSmallIcon(R.mipmap.ic_launcher_circle)
            setContentTitle(context.getString(R.string.ntf_mission_start_title))
            setContentText(
                when {
                    incentives > 0 -> context.getString(
                        R.string.ntf_mission_start_text_gain, durationMinutes, incentives
                    )
                    incentives < 0 -> context.getString(
                        R.string.ntf_mission_start_text_loss, durationMinutes, incentives
                    )
                    else -> context.getString(
                        R.string.ntf_mission_start_text_none, durationMinutes
                    )
                }
            )
        }.build()
    }

    fun buildMissionSuccessNotification(
        context: Context,
        incentives: Int
    ): Notification {
        val builder =
            getNotificationBuilder(context, CHANNEL_ID_MISSION_SUCCESS)
                ?: NotificationCompat.Builder(
                    context,
                    CHANNEL_ID_MISSION_SUCCESS
                )
        return builder.apply {
            setOngoing(false)
            setLocalOnly(false)
            setAutoCancel(true)
            setSmallIcon(R.mipmap.ic_launcher_circle)
            setContentTitle(context.getString(R.string.ntf_mission_success_title))
            setContentText(
                when {
                    incentives > 0 -> context.getString(
                        R.string.ntf_mission_success_text_gain, incentives
                    )
                    incentives < 0 -> context.getString(
                        R.string.ntf_mission_success_text_loss, incentives
                    )
                    else -> context.getString(
                        R.string.ntf_mission_success_text_none
                    )
                }
            )
        }.build()
    }

    fun buildMissionFailureNotification(
        context: Context,
        incentives: Int
    ): Notification {
        val builder =
            getNotificationBuilder(context, CHANNEL_ID_MISSION_FAILURE)
                ?: NotificationCompat.Builder(
                    context,
                    CHANNEL_ID_MISSION_FAILURE
                )
        return builder.apply {
            setOngoing(false)
            setLocalOnly(false)
            setAutoCancel(true)
            setSmallIcon(R.mipmap.ic_launcher_circle)
            setContentTitle(context.getString(R.string.ntf_mission_failure_title))
            setContentText(
                when {
                    incentives > 0 -> context.getString(
                        R.string.ntf_mission_failure_text_gain, incentives
                    )
                    incentives < 0 -> context.getString(
                        R.string.ntf_mission_failure_text_loss, incentives
                    )
                    else -> context.getString(
                        R.string.ntf_mission_failure_text_none
                    )
                }
            )
        }.build()
    }

    fun notifyMissionStart(
        context: Context,
        incentives: Int,
        durationMinutes: Long,
        countDownUntil: Long
    ) {
        val notification = buildMissionStartNotification(
            context = context,
            incentives = incentives,
            durationMinutes = durationMinutes,
            countDownUntil = countDownUntil
        )
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MISSION, notification)
    }

    fun notifyMissionResult(
        context: Context,
        incentives: Int,
        isSucceeded: Boolean
    ) {
        val notification = if (isSucceeded) {
            buildMissionSuccessNotification(
                context = context,
                incentives = incentives
            )
        } else {
            buildMissionSuccessNotification(
                context = context,
                incentives = incentives
            )
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MISSION, notification)
    }
}