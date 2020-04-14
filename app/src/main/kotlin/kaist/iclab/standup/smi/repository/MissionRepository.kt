package kaist.iclab.standup.smi.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import androidx.core.app.AlarmManagerCompat
import com.fonfon.kgeohash.GeoHash
import com.google.firebase.firestore.CollectionReference
import kaist.iclab.standup.smi.common.boundingBoxGeoHashes
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.data.Missions
import java.util.*
import java.util.concurrent.TimeUnit

class SessionRepository (
    context: Context,
    private val docFactory: (() -> CollectionReference)? = null,
    private val initFactory: (id: String?) -> PendingIntent,
    private val sessionFactory: (id: String?) -> PendingIntent,
    private val expireFactory: (id: String?) -> PendingIntent,
    private val incentiveFactory: suspend SessionRepository.() -> Int
) {
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun cancelAll() {
        val initIntent = initFactory.invoke(null)
        val sessionIntent = sessionFactory.invoke(null)
        val expireIntent = expireFactory.invoke(null)

        alarmManager.apply {
            cancel(initIntent)
            cancel(sessionIntent)
            cancel(expireIntent)
        }
    }

    suspend fun getSessions(latitude: Double, longitude: Double, distanceInMetre: Float, limit: Long? = null): List<Mission> {
        val reference = docFactory?.invoke() ?: return listOf()

        val (minGeoHash, maxGeoHash) = boundingBoxGeoHashes(latitude, longitude, distanceInMetre)

        return Mission.select(
            ref = reference,
            orderBy = Missions.deliveredTime,
            isAscending = false,
            limit = limit
        ) {
            Missions.geoHash greaterThanOrEqualTo minGeoHash
            Missions.geoHash lessThan maxGeoHash
        }
    }

    suspend fun getSessions(fromTime: Long, toTime: Long, limit: Long? = null): List<Mission> {
        val reference = docFactory?.invoke() ?: return listOf()

        return Mission.select(
            ref = reference,
            orderBy = Missions.deliveredTime,
            isAscending = false,
            limit = limit
        ) {
            Missions.deliveredTime greaterThanOrEqualTo fromTime
            Missions.deliveredTime lessThan toTime
        }
    }

    suspend fun getSessions(timeRanges: List<Pair<Long, Long>>): Map<Pair<Long, Long>, List<Mission>> {
        val fromTime = timeRanges.minBy { it.first }?.first ?: return mapOf()
        val toTime = timeRanges.maxBy { it.second }?.second ?: return mapOf()
        val interventions = getSessions(fromTime, toTime)
        return timeRanges.associateWith { (from, to) ->
            interventions.filter { it.deliveredTime in (from until to) }
        }
    }

    suspend fun startSession(
        startTime: Long,
        location: Location,
        timeout: Long
    ) {
        val ref = docFactory?.invoke() ?: return
        val incentive = incentiveFactory.invoke(this)

        val id = Mission.create(ref) {
            this.offsetMs = TimeZone.getDefault().rawOffset
            this.latitude = location.latitude
            this.longitude = location.longitude
            this.geoHash = GeoHash(location, 8).toString()
            this.startTime = startTime
            this.incentive = incentive
        }

        val intent = initFactory.invoke(id)
        schedule(startTime + timeout, intent)
    }

    suspend fun standBySession(
        id: String,
        timeout: Long
    ) {
        val ref = docFactory?.invoke() ?: return
        Mission.update(ref, id) {
            state = Mission.STATE_STAND_BY
        }
        val session = Mission.get(ref, id)
        val startTime = session?.startTime ?: System.currentTimeMillis()
        val intent = sessionFactory.invoke(id)

        schedule(startTime + timeout, intent)
    }

    suspend fun startMission(
        id: String,
        timeout: Long
    ) {
        val ref = docFactory?.invoke() ?: return
        val curTime = System.currentTimeMillis()

        Mission.update(ref, id) {
            deliveredTime = curTime
            state = Mission.STATE_ON_MISSION
        }
        val session = Mission.get(ref, id)
        val startTime = session?.startTime ?: System.currentTimeMillis()
        val intent = expireFactory.invoke(id)

        schedule(startTime + timeout, intent)
    }

    suspend fun succeedMission(
        id: String,
        timestamp: Long
    ) {
        cancelAll()

        val ref = docFactory?.invoke() ?: return

        Mission.update(ref, id) {
            state = Mission.STATE_SUCCESS
            reactionTime = timestamp
        }
    }

    suspend fun failureMission(
        id: String,
        timestamp: Long
    ) {
        val ref = docFactory?.invoke() ?: return

        Mission.update(ref, id) {
            state = Mission.STATE_FAILURE
            reactionTime = timestamp
        }
    }

    suspend fun cheatMission(
        id: String,
        timestamp: Long
    ) {
        val ref = docFactory?.invoke() ?: return

        Mission.update(ref, id) {
            state = Mission.STATE_CHEAT
            reactionTime = timestamp
        }
    }

    private fun schedule(triggerAt: Long, intent: PendingIntent) {
        val minTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10)

        cancelAll()

        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            triggerAt.coerceAtLeast(minTime),
            intent
        )
    }
}