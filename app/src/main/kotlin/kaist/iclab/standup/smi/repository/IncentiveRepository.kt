package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.pref.RemotePrefs

abstract class IncentiveCalculator(
    private val missionRepository: MissionRepository
) {
    fun getFixedIncentive(): Int = RemotePrefs.defaultIncentives

    suspend fun getStochasticIncentive(
        curTime: Long,
        curLatitude: Double,
        curLongitude: Double,
        winSizeInMillis: Long,
        winSizeInNumber: Long,
        minIncentive: Int,
        maxIncentive: Int,
        defaultIncentive: Int,
        unitIncentive: Int
    ): Int {
        val curTime = System.currentTimeMillis()
        val missions = missionRepository.getMissionsByDeliveredTime(
            fromTime = curTime - winSizeInMillis,
            toTime = curTime,
        ).takeLast(winSizeInNumber.toInt())
    }

    abstract fun calculateStochasticIncentive(
        previousMissions: List<Mission>,
        curTime: Long,
        curLatitude: Double,
        curLongitude: Double
    ): Int


}