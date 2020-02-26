package kaist.iclab.standup.smi.base

import android.app.Service
import android.content.Intent
import kaist.iclab.standup.smi.common.AppLog

abstract class BaseService: Service() {
    override fun onCreate() {
        super.onCreate()
        AppLog.d(javaClass, "onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.d(javaClass, "onStartCommand(intent: $intent, flags: $flags, startId: $startId)")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        AppLog.d(javaClass, "onDestroy()")
        super.onDestroy()
    }

    override fun onLowMemory() {
        AppLog.d(javaClass, "onLowMemory()")
        super.onLowMemory()
    }
}