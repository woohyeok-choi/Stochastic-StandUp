package kaist.iclab.standup.smi.repository

data class SedentaryDurationEvent(
    val startTime: Long?,
    val endTime: Long?,
    val duration: Long,
    val geoHash: String?,
    val latitude: Double?,
    val longitude: Double?
)
