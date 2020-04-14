package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.data.Mission
import java.util.concurrent.TimeUnit

interface IncentiveRepository {
    fun calculateStochasticIncentive(
        missions: List<Mission>,
        timestamp: Long,
        latitude: Double,
        longitude: Double
    ): Int?
}