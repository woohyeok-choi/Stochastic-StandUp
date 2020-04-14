package kaist.iclab.standup.smi.common

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.PowerManager
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.tasks.Task
import io.reactivex.Single
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import kotlin.coroutines.*
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
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

fun Context.checkWhitelist() : Boolean {
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


fun Context.toast(msg: String, isShort: Boolean = true) {
    Toast.makeText(this, msg, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Context.toast(res: Int, isShort: Boolean = true) {
    Toast.makeText(this, res, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Context.toast(res: Int, vararg params: Any, isShort: Boolean = true) {
    Toast.makeText(this, getString(res, *params), if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Context.toast(throwable: Throwable?, isShort: Boolean = true) {
    Toast.makeText(this, wrapError(throwable).toString(this), if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

fun Fragment.toast(msg: String, isShort: Boolean = true) {
    context?.toast(msg, isShort)
}

fun Fragment.toast(res: Int, isShort: Boolean = true) {
    context?.toast(res, isShort)
}

fun Fragment.toast(res: Int, vararg params: Any, isShort: Boolean = true) {
    context?.toast(res, params, isShort)
}

fun Fragment.toast(throwable: Throwable?, isShort: Boolean = true) {
    context?.toast(throwable, isShort)
}

fun <K, V> MutableMap<K, V>.mergeValue(key: K, value: V, func: (V, V) -> V?) {
    val oldValue = get(key)
    val newValue = if (oldValue == null) value else func.invoke(oldValue, value)
    if (newValue == null) remove(key) else put(key, newValue)
}

fun <K, V> MutableMap<K, MutableList<V>>.appendValues(key: K, value: V) {
    val list = get(key) ?: mutableListOf()
    list.add(value)
    put(key, list)
}

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: Long = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}


fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : Float {
    val result = floatArrayOf(0F)
    Location.distanceBetween(lat1, lon1, lat2, lon2, result)
    return result.first()
}

fun regularId(timestamp: Long) : String = DateTime(timestamp, DateTimeZone.UTC).toString(ISODateTimeFormat.dateTime())

fun floorDiv(x: Int, y: Int): Int {
    var r = x / y
    // if the signs are different and modulo not zero, round down
    if (x xor y < 0 && r * y != x) {
        r--
    }
    return r
}

fun Context.getResourceUri(@AnyRes id: Int): Uri = Uri.Builder()
    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
    .authority(packageName)
    .path(id.toString())
    .build()


fun Pair<Double, Double>.toGeoHash() : String {
    val lat = first
    val lon = second
    return if (!lat.isNaN() || !lon.isNaN()) "" else GeoHash(lat, lon, 8).toString()
}

fun Location.toGeoHash() = this.let { GeoHash(this, 8).toString() }

fun Pair<Double, Double>.toBoundingBoxGeoHash(distanceInMetre: Float) : Pair<String, String> {
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

fun Location.toBoundingBoxGeoHash(distanceInMetre: Float) = (latitude to longitude).toBoundingBoxGeoHash(distanceInMetre)