package kaist.iclab.standup.smi.tracker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.*
import kaist.iclab.standup.smi.common.asSuspend

class LocationTracker(private val context: Context,
                      private val request: LocationRequest,
                      private val pendingIntent: PendingIntent) {
    private val locationClient: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun startTracking() {
        locationClient.requestLocationUpdates(request, pendingIntent)
    }

    fun stopTracking() {
        locationClient.removeLocationUpdates(pendingIntent)
    }

    fun extract(intent: Intent) : Location? {
        if (!LocationResult.hasResult(intent)) return null
        return LocationResult.extractResult(intent).lastLocation
    }

    suspend fun getLastLocation() : Location? = locationClient.lastLocation?.asSuspend()
}