package kaist.iclab.standup.smi.common

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.tasks.Task
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import org.koin.android.ext.android.getKoin
import org.koin.androidx.viewmodel.koin.getViewModel
import org.koin.core.qualifier.Qualifier
import smile.stat.distribution.Distribution
import smile.stat.distribution.GaussianDistribution
import smile.stat.distribution.TDistribution
import java.util.concurrent.TimeUnit
import kotlin.coroutines.*
import kotlin.math.*
import io.reactivex.rxjava3.core.Single as Rx3Single

fun Context.safeRegisterReceiver(receiver: BroadcastReceiver, filter: IntentFilter) = try {
    registerReceiver(receiver, filter)
} catch (e: IllegalArgumentException) {
}

fun Context.safeUnregisterReceiver(receiver: BroadcastReceiver) = try {
    unregisterReceiver(receiver)
} catch (e: IllegalArgumentException) {
}

fun Context.checkPermissions(permissions: Collection<String>): Boolean =
    if (permissions.isEmpty()) {
        true
    } else {
        permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

fun Context.checkPermissions(permissions: Array<String>): Boolean =
    if (permissions.isEmpty()) {
        true
    } else {
        permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

fun Context.checkWhitelist(): Boolean {
    val manager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return manager.isIgnoringBatteryOptimizations(packageName)
}

suspend fun <T : Any> Rx3Single<T>.asSuspend(
    context: CoroutineContext = EmptyCoroutineContext,
    throwable: Throwable? = null
) = withContext(context) {
    suspendCoroutine<T> { continuation ->
        subscribe { result, exception ->
            if (exception != null) {
                continuation.resumeWithException(throwable ?: exception)
            } else {
                continuation.resume(result)
            }
        }
    }
}

suspend fun <T : Any> Single<T>.asSuspend(
    context: CoroutineContext = EmptyCoroutineContext,
    throwable: Throwable? = null
) = withContext(context) {
    suspendCoroutine<T> { continuation ->
        subscribe { result, exception ->
            if (exception != null) {
                continuation.resumeWithException(throwable ?: exception)
            } else {
                continuation.resume(result)
            }
        }
    }
}

suspend fun <T : Any> Task<T?>.asSuspend(
    context: CoroutineContext = EmptyCoroutineContext,
    throwable: Throwable? = null
) = withContext(context) {
    suspendCoroutine<T?> { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(throwable ?: task.exception ?: Exception())
            }
        }
    }
}

fun SupportMapFragment.doMapOperation(op: (map: GoogleMap) -> Unit) {
    getMapAsync {
        op.invoke(it)
    }
}

suspend fun SupportMapFragment.getMap(init: (map: GoogleMap) -> Unit) : GoogleMap = suspendCoroutine { continuation ->
    getMapAsync {
        init.invoke(it)
        continuation.resume(it)
    }
}

fun Context.toast(msg: String, isShort: Boolean = true) {
    Toast.makeText(this, msg, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Context.toast(@StringRes resId: Int, isShort: Boolean = true) =
    toast(getString(resId), isShort)

fun Context.toast(@StringRes resId: Int, vararg params: Any, isShort: Boolean = true) =
    toast(getString(resId, *params), isShort)

fun Context.toast(throwable: Throwable?, isShort: Boolean = true) =
    toast(throwable.wrap().toString(this), isShort)

fun Fragment.toast(msg: String, isShort: Boolean = true) = context?.toast(msg, isShort)

fun Fragment.toast(@StringRes resId: Int, isShort: Boolean = true) = context?.toast(resId, isShort)

fun Fragment.toast(@StringRes resId: Int, vararg params: Any, isShort: Boolean = true) =
    context?.toast(resId, params, isShort)

fun Fragment.toast(throwable: Throwable?, isShort: Boolean = true) =
    context?.toast(throwable, isShort)

fun snackBar(
    view: View,
    msg: String,
    isShort: Boolean = true,
    @IdRes anchorId: Int? = null,
    actionName: String? = null,
    action: (() -> Unit)? = null
) {
    try {
        val snackBar =
            Snackbar.make(view, msg, if (isShort) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG)

        if (anchorId != null) {
            snackBar.setAnchorView(anchorId)
        }

        if (actionName != null && action != null) {
            snackBar.setAction(actionName) { action.invoke() }
        }

        snackBar.show()
    } catch (e: Exception) {  }
}

fun Context.snackBar(
    view: View,
    @StringRes resId: Int,
    isShort: Boolean = true,
    @IdRes anchorId: Int? = null,
    @StringRes actionResId: Int? = null,
    action: (() -> Unit)? = null
) = snackBar(view, getString(resId), isShort, anchorId, actionResId?.let { getString(it) }, action)

fun Context.snackBar(
    view: View,
    @StringRes resId: Int,
    isShort: Boolean = true,
    vararg params: Any,
    @IdRes anchorId: Int? = null,
    @StringRes actionResId: Int? = null,
    action: (() -> Unit)? = null
) = snackBar(
    view,
    getString(resId, *params),
    isShort,
    anchorId,
    actionResId?.let { getString(it) },
    action
)

fun Fragment.snackBar(
    view: View,
    @StringRes resId: Int,
    isShort: Boolean = true,
    @IdRes anchorId: Int? = null,
    @StringRes actionResId: Int? = null,
    action: (() -> Unit)? = null
) = snackBar(view, getString(resId), isShort, anchorId, actionResId?.let { getString(it) }, action)

fun Fragment.snackBar(
    view: View,
    @StringRes resId: Int,
    isShort: Boolean = true,
    vararg params: Any,
    @IdRes anchorId: Int? = null,
    @StringRes actionResId: Int? = null,
    action: (() -> Unit)? = null
) = snackBar(
    view,
    getString(resId, *params),
    isShort,
    anchorId,
    actionResId?.let { getString(it) },
    action
)

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: Long = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun Long.toISODateTime(): String =
    DateTime(this, DateTimeZone.UTC).toString(ISODateTimeFormat.dateTime())

fun Context.getResourceUri(@AnyRes id: Int): Uri = Uri.Builder()
    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
    .authority(packageName)
    .path(id.toString())
    .build()

/**
 * @param nChars the number of GeoHash characters
 *  5: +-2.4 km error
 *  6: +-0.61 km error
 *  7: +-0.076 km error
 *  8: +-0.019 km error
 */
fun Pair<Double, Double>.toGeoHash(nChars: Int = 7): String? {
    val lat = first
    val lon = second
    return if (lat.isNaN() || lon.isNaN()) null else GeoHash(lat, lon, nChars).toString()
}

fun Location.toGeoHash() = GeoHash(this, 8).toString()

fun String.toLatLng(): Pair<Double, Double> =
    GeoHash(this).toLocation().let { it.latitude to it.longitude }

fun Pair<Double, Double>.toBoundingBoxGeoHash(distanceInMetre: Float): Pair<String, String> {
    val latitude = first
    val longitude = second

    val r = distanceInMetre / 6378137.0
    val lat = latitude * Math.PI / 180.0
    val lon = longitude * Math.PI / 180.0

    val latMin = lat - r
    val latMax = lat + r

    val latT = asin(sin(lat) / cos(r))
    val a = cos(r) - sin(latT) * sin(lat)
    val b = cos(latT) * cos(lat)
    val lonD = acos(a / b)

    val lonMin = (lon - lonD).coerceAtLeast(-Math.PI)
    val lonMax = (lon + lonD).coerceAtMost(Math.PI)

    val latMinDeg = latMin * 180.0 / Math.PI
    val latMaxDeg = latMax * 180.0 / Math.PI
    val lonMinDeg = lonMin * 180.0 / Math.PI
    val lonMaxDeg = lonMax * 180.0 / Math.PI

    val minGeoHash = GeoHash(latMinDeg, lonMinDeg, 8).toString()
    val maxGeoHash = GeoHash(latMaxDeg, lonMaxDeg, 8).toString()

    return minGeoHash to maxGeoHash
}

fun Long.millisToHourMinute() : Pair<Long, Long> {
    var value = this
    val hourMs = TimeUnit.HOURS.toMillis(1)
    val minuteMs = TimeUnit.MINUTES.toMillis(1)

    var hourValue = 0L
    var minuteValue = 0L

    if (value > hourMs) {
        hourValue = value / hourMs
        value -= hourValue * hourMs
    }

    if (value > minuteMs) {
        minuteValue = value / minuteMs
    }

    return hourValue to minuteValue
}

fun Pair<Long, Long>.hourMinuteToMillis() : Long {
    val hourMs = TimeUnit.HOURS.toMillis(1)
    val minuteMs = TimeUnit.MINUTES.toMillis(1)
    return first * hourMs + second * minuteMs
}

fun Pair<Long, Long>.hourMinuteToString() = String.format("%02d:%02d", first, second)

fun CharSequence?.toHourMinutes() : Pair<Long, Long> {
    val parts = this?.split(":") ?: return 0L to 0L
    return try {
        parts[0].toLong() to parts[1].toLong()
    } catch (e: Exception) {
        0L to 0L
    }
}

inline fun <reified T: ViewModel> Fragment.sharedViewModelFromFragment(
    qualifier: Qualifier? = null
): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) {
        getKoin().getViewModel(
            requireParentFragment(),
            T::class,
            qualifier,
            null
        )
    }

fun Context.scheduleAlarm(
    triggerAt: Long,
    intent: PendingIntent,
    minOffset: Long = TimeUnit.SECONDS.toMillis(5)
) {
    val minTime = System.currentTimeMillis() + minOffset
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    AlarmManagerCompat.setExactAndAllowWhileIdle(
        alarmManager,
        AlarmManager.RTC_WAKEUP,
        triggerAt.coerceAtLeast(minTime),
        intent
    )
}

fun Context.cancelAlarm(
    intent: PendingIntent
) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(intent)
}

fun Collection<Number>.confidenceInterval(
    alpha: Double,
    isSample: Boolean
) : Double {
    val n = size
    if (n == 0) return 0.0
    var sumOfValue = 0.0
    var sumOfValueSquare = 0.0

    for (i in this) {
        val v = i.toDouble()
        sumOfValue += v
        sumOfValueSquare += v * v
    }

    val mean = sumOfValue / n
    var variance = sumOfValueSquare / n - mean * mean

    val useTDist = isSample && n > 1

    if (useTDist) variance = variance * n / (n - 1)

    val stdDev = sqrt(variance)

    val distribution: Distribution = if (useTDist) {
        TDistribution(n - 1)
    } else {
        GaussianDistribution(0.0, 1.0)
    }

    val p = alpha / 2
    val criticalValue = distribution.quantile(p)

    return abs(criticalValue * stdDev / sqrt(n.toDouble()))
}

suspend fun <R> tryCatch(block: suspend () -> R) = try { block.invoke() } catch (e: Exception) { null }

suspend fun Context.checkNetworkConnection() : Boolean = suspendCoroutine { continuation ->
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Log.d("checkNetworkConnection()", "Using activeNetworkInfo")
        val activeNetwork = manager.activeNetworkInfo
        continuation.resume(activeNetwork?.isConnectedOrConnecting == true)
    } else {
        Log.d("checkNetworkConnection()", "Using networkCallback")
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("checkNetworkConnection()", "onAvailable")
                manager.unregisterNetworkCallback(this)
                continuation.resume(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("checkNetworkConnection()", "onLost")
                manager.unregisterNetworkCallback(this)
                continuation.resume(false)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
    }
}