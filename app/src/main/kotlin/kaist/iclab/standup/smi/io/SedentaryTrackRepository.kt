package kaist.iclab.standup.smi.io

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import androidx.core.app.AlarmManagerCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.DocumentReference
import kaist.iclab.standup.smi.BuildConfig
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.common.asSuspend
import kaist.iclab.standup.smi.common.safeRegisterReceiver
import kaist.iclab.standup.smi.common.safeUnregisterReceiver
import kaist.iclab.standup.smi.common.throwError
import kaist.iclab.standup.smi.io.data.Event
import kaist.iclab.standup.smi.io.data.Events
import kaist.iclab.standup.smi.io.pref.DebugPrefs
import kaist.iclab.standup.smi.io.pref.LocalPrefs
import kaist.iclab.standup.smi.io.pref.RemotePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SedentaryTrackRepository(private val context: Context, private val referenceFactory: () -> DocumentReference?) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val activityClient: ActivityRecognitionClient by lazy { ActivityRecognition.getClient(context) }
    private val locationClient: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val alarmManager: AlarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    private val activityRequest = listOf(
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()
    ).let { ActivityTransitionRequest(it) }

    private val locationRequest = LocationRequest.create()
        .setInterval(TimeUnit.SECONDS.toMillis(30))
        .setSmallestDisplacement(5.0F)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    private val activityIntent: PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE,
        Intent(ACTION_ACTIVITY_TRANSITION_UPDATE),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val locationIntent: PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE_LOCATION_UPDATE,
        Intent(ACTION_LOCATION_UPDATE),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val intentFilter = IntentFilter().apply {
        addAction(ACTION_ACTIVITY_TRANSITION_UPDATE)
        addAction(ACTION_LOCATION_UPDATE)
        addAction(ACTION_INTERVENTION_DELIVERY)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_ACTIVITY_TRANSITION_UPDATE -> handleActivityTransitionRetrieval(intent)
            }
        }
    }

    fun start() {
        context.safeRegisterReceiver(receiver, intentFilter)

        locationClient.requestLocationUpdates(locationRequest, locationIntent)
        activityClient.requestActivityTransitionUpdates(activityRequest, activityIntent)

        newSedentaryEvent(System.currentTimeMillis())
    }

    fun stop() {
        context.safeUnregisterReceiver(receiver)

        locationClient.removeLocationUpdates(locationIntent)
        activityClient.removeActivityTransitionUpdates(activityIntent)

        newStandUpEvent(System.currentTimeMillis())
    }

    private fun handleActivityTransitionRetrieval(intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val timestamp = System.currentTimeMillis()
        val results = ActivityTransitionResult.extractResult(intent)?.transitionEvents?.filter { event ->
            event.activityType == DetectedActivity.STILL
        } ?: return

        results.forEach { event ->
            try {
                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) newSedentaryEvent(timestamp) else newStandUpEvent(timestamp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun newSedentaryEvent(timestamp: Long) = launch {
        val lastLocation = locationClient.lastLocation?.asSuspend()
        val historyId = Event.create(referenceFactory.invoke()) {
            startTime = timestamp
            latitude = lastLocation?.latitude
            longitude = lastLocation?.longitude
        } ?: ""

        LocalPrefs.lastEventId = historyId

        val sedentaryThreshold = if (BuildConfig.DEBUG) DebugPrefs.sedentaryThreshold else RemotePrefs.sedentaryThreshold

        schedule(timestamp + sedentaryThreshold, historyId)
    }

    private fun newStandUpEvent(timestamp: Long) = launch {
        Event.update(referenceFactory.invoke(), LocalPrefs.lastEventId) {
            endTime = timestamp
        }

        cancel()
    }

    private fun schedule(triggerAt: Long, historyId: String) {
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_INTERVENTION_DELIVERY,
            Intent(ACTION_INTERVENTION_DELIVERY).putExtra(EXTRA_HISTORY_ID, historyId),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            intent
        )
    }

    private fun cancel() {
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_INTERVENTION_DELIVERY,
            Intent(ACTION_INTERVENTION_DELIVERY),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(intent)
    }

    private suspend fun calculateStochasticIncentives(historyId: String) : Float {
        val timestamp = System.currentTimeMillis()
        val curHistory = Event.get(referenceFactory.invoke(), historyId) ?: throwError(R.string.error_no_matched_entity)
        val curLatitude = curHistory.latitude ?: throwError(R.string.error_no_matched_field, Events.latitude.name)
        val curLongitude = curHistory.latitude ?: throwError(R.string.error_no_matched_field, Events.longitude.name)
        val windowSize = if(BuildConfig.DEBUG) DebugPrefs.windowSize else RemotePrefs.windowSize
        val distanceThreshold = if(BuildConfig.DEBUG) DebugPrefs.distanceThreshold else RemotePrefs.distanceThreshold
        val sedentaryThreshold = if (BuildConfig.DEBUG) DebugPrefs.sedentaryThreshold else RemotePrefs.sedentaryThreshold

        val histories = Event.select(referenceFactory.invoke()) {
            Events.startTime greaterThanOrEqualTo timestamp - windowSize
            Events.endTime greaterThanOrEqualTo 0
        }.filter { history ->
            val startTime = history.startTime ?: 0
            val endTime = history.endTime ?: 0
            val latitude = history.latitude ?: Double.MAX_VALUE
            val longitude = history.longitude ?: Double.MAX_VALUE

            val isSedentary = (endTime - startTime) >= sedentaryThreshold


            val isNearby = dist(
                lat1 = curLatitude, lon1 = curLongitude,
                lat2 = latitude, lon2 = longitude
            ) < distanceThreshold

            return@filter isSedentary && isNearby
        }
        TODO("Think how to define an amount of incentives")
    }

    private fun dist(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : Float {
        val result = floatArrayOf(0.0F)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result.first()
    }

    companion object {
        private const val REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE = 0xf1
        private const val REQUEST_CODE_LOCATION_UPDATE = 0xf2
        private const val REQUEST_CODE_INTERVENTION_DELIVERY = 0xf3

        private const val ACTION_ACTIVITY_TRANSITION_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_ACTIVITY_TRANSITION_UPDATE"

        private const val ACTION_LOCATION_UPDATE =
            "${BuildConfig.APPLICATION_ID}.ACTION_LOCATION_UPDATE"

        private const val ACTION_INTERVENTION_DELIVERY =
            "${BuildConfig.APPLICATION_ID}.REQUEST_CODE_INCENTIVE_DELIVERY"

        private const val EXTRA_HISTORY_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_HISTORY_ID"
    }
}