package kaist.iclab.standup.smi.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.PowerManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Task
import io.reactivex.Single
import io.reactivex.rxjava3.core.Single as Rx3Single
import kotlinx.coroutines.withContext
import kotlin.coroutines.*

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


