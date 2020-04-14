package kaist.iclab.standup.smi.tracker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.*


class ActivityTracker(
    private val context: Context,
    private val request: ActivityTransitionRequest,
    private val pendingIntent: PendingIntent
) {
    private val activityClient: ActivityRecognitionClient by lazy {
        ActivityRecognition.getClient(
            context
        )
    }

    fun startTracking() {
        activityClient.requestActivityTransitionUpdates(request, pendingIntent)
    }

    fun stopTracking() {
        activityClient.removeActivityTransitionUpdates(pendingIntent)
    }

    fun removeRequest(intent: PendingIntent) {
        activityClient.removeActivityTransitionUpdates(intent)
    }

    fun extract(intent: Intent?): List<ActivityTransitionEvent> {
        if (intent == null || !ActivityTransitionResult.hasResult(intent)) return listOf()
        return ActivityTransitionResult.extractResult(intent)?.transitionEvents?.sortedWith(comparator) ?: listOf()
    }

    private val comparator: Comparator<ActivityTransitionEvent> = Comparator { o1, o2 ->
        val compTime = o1.elapsedRealTimeNanos.compareTo(o2.elapsedRealTimeNanos)
        if (compTime != 0) return@Comparator compTime
        val compActivityType = o1.activityType.compareTo(o2.activityType)
        if (compActivityType != 0) return@Comparator compActivityType
        return@Comparator o1.transitionType.compareTo(o2.transitionType)
    }
}