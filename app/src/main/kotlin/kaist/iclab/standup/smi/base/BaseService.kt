package kaist.iclab.standup.smi.base

import android.app.Service
import android.content.Intent
import androidx.annotation.CallSuper
import kaist.iclab.standup.smi.common.AppLog

abstract class BaseService: Service() {
    @CallSuper
    override fun onCreate() {
        super.onCreate()
        AppLog.d(javaClass, "onCreate()")
    }

    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.d(javaClass, "onStartCommand(intent: $intent, flags: $flags, startId: $startId)")
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onDestroy() {
        AppLog.d(javaClass, "onDestroy()")
        super.onDestroy()
    }

    @CallSuper
    override fun onLowMemory() {
        AppLog.d(javaClass, "onLowMemory()")
        super.onLowMemory()
    }
}