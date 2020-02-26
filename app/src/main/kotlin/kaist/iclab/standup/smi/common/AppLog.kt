package kaist.iclab.standup.smi.common

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.reflect.KClass

object AppLog {
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun d(clazz: Class<*>, msg: String) {
        Log.d(clazz.name, msg)
    }

    fun d(clazz: KClass<*>, msg: String) {
        Log.d(clazz.java.name, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        throwable?.printStackTrace()
        Log.e(tag, msg)
    }

    fun e(clazz: Class<*>, msg: String, throwable: Throwable? = null) {
        throwable?.printStackTrace()
        Log.e(clazz.name, msg)
    }

    fun e(clazz: KClass<*>, msg: String, throwable: Throwable? = null) {
        throwable?.printStackTrace()
        Log.e(clazz.java.name, msg)
    }

    fun ee(tag: String, msg: String, throwable: Throwable? = null) {
        throwable?.let {
            it.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(it)
        }
        throwable?.printStackTrace()
        Log.e(tag, msg)

    }

    fun ee(clazz: Class<*>, msg: String, throwable: Throwable? = null) {
        throwable?.let {
            it.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(it)
        }
        Log.e(clazz.name, msg)
    }

    fun ee(clazz: KClass<*>, msg: String, throwable: Throwable? = null) {
        throwable?.let {
            it.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(it)
        }
        Log.e(clazz.java.name, msg)
    }
}