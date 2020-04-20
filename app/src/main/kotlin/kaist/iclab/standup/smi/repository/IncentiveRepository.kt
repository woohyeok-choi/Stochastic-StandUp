package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.data.Mission

interface IncentiveRepository {
    fun calculateStochasticIncentive(
        histories: List<IncentiveHistory>,
        timestamp: Long,
        latitude: Double,
        longitude: Double
    ): Int?
}