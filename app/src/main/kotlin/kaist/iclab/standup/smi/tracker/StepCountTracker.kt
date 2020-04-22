package kaist.iclab.standup.smi.tracker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import kaist.iclab.standup.smi.common.AppLog
import kaist.iclab.standup.smi.pref.LocalPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.TimeUnit

class StepCountTracker (
    private val context: Context,
    private val fitnessOptions: FitnessOptions,
    private val pendingIntent: PendingIntent
): OnDataPointListener {
    private val subject: PublishSubject<DataPoint> = PublishSubject.create()
    private var disposable: Disposable? = null
    private val scheduler = Schedulers.from(Dispatchers.IO.asExecutor())

    fun startTracking() {
        LocalPrefs.isSedentary = true

        val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
        val winSize = TimeUnit.SECONDS.toMillis(60)
        val minStepCounts = 10
        val maxStepCounts = 45
        val samplingPeriod = TimeUnit.SECONDS.toMillis(5)
        val timeShift = TimeUnit.SECONDS.toMillis(15)
        /**
         * ..10 -   GRAY REGION -   60..
         * SED  -   UNDEFINED   -   STAND_UP
         */

        disposable?.dispose()
        scheduler.start()

        disposable = subject.buffer(winSize, timeShift, TimeUnit.MILLISECONDS, scheduler).subscribe { data ->
            val steps = data.sumBy { it.getValue(Field.FIELD_STEPS)?.asInt() ?: 0 }
            val latestEventTime = data.map { it.getTimestamp(TimeUnit.MILLISECONDS) }.max() ?: System.currentTimeMillis()
            val prevIsSedentary = LocalPrefs.isSedentary

            val isSedentary = when {
                steps < minStepCounts -> {
                    true
                }
                steps >= maxStepCounts -> {
                    false
                }
                else -> {
                    prevIsSedentary
                }
            }
            AppLog.d(javaClass, "subscribe(): eventTime = $latestEventTime, Steps = $steps, isSedentary = $isSedentary, prevIsSedentary = $prevIsSedentary")

            if (isSedentary != prevIsSedentary) {
                LocalPrefs.isSedentary = isSedentary

                sendEvent(isSedentary, if (isSedentary) System.currentTimeMillis() else latestEventTime)
            }
        }

        Fitness.getSensorsClient(context, account).add(
            SensorRequest.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setSamplingRate(samplingPeriod, TimeUnit.MILLISECONDS)
                .build(),
            this
        ).addOnFailureListener {
            AppLog.e(javaClass, "startTracking()", it)
        }

        sendEvent(true, System.currentTimeMillis())
    }

    fun stopTracking() {
        val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
        Fitness.getSensorsClient(context, account).remove(this)
        disposable?.dispose()
        scheduler.shutdown()
    }

    override fun onDataPoint(point: DataPoint?) {
        val steps = point?.getValue(Field.FIELD_STEPS)?.asInt() ?: 0
        val timestamp = point?.getTimestamp(TimeUnit.MILLISECONDS)
        val startTime = point?.getStartTime(TimeUnit.MILLISECONDS)
        val endTime = point?.getEndTime(TimeUnit.MILLISECONDS)

        AppLog.d(javaClass, "onDataPoint: $steps at $timestamp ($startTime - $endTime)")

        if (point != null) {
            subject.onNext(point)
        }
    }

    fun extract(intent: Intent): Pair<Boolean, Long> {
        val isSedentary = intent.getBooleanExtra(EXTRA_IS_ENTERED, true)
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        return isSedentary to timestamp
    }

    fun fillExtra(extra: Bundle, isSedentary: Boolean, timestamp: Long) : Bundle {
        extra.putBoolean(EXTRA_IS_ENTERED, isSedentary)
        extra.putLong(EXTRA_TIMESTAMP, timestamp)
        return extra
    }

    private fun sendEvent(isSedentary: Boolean, timestamp: Long) {
        pendingIntent.send(
            context, 0x00, Intent()
                .putExtra(EXTRA_IS_ENTERED, isSedentary)
                .putExtra(EXTRA_TIMESTAMP, timestamp)
        )
    }

    companion object {
        private val EXTRA_IS_ENTERED = "${StepCountTracker::class.java.name}.EXTRA_IS_ENTERED"
        private val EXTRA_TIMESTAMP = "${StepCountTracker::class.java.name}.EXTRA_TIMESTAMP"
    }
}