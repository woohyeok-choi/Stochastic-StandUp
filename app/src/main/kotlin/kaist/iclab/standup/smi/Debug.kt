package kaist.iclab.standup.smi

import android.content.res.Resources
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Debug {
    suspend fun generateDebugData(repository: StandUpMissionHandler, resources: Resources) = withContext(Dispatchers.IO) {
        resources.openRawResource(R.raw.sedentary_events).bufferedReader().readLines().forEach { line ->
            val parts = line.split(",").map { it.trim() }
            try {
                val timestamp = parts[1].toLong()
                val isEntered = parts[2].startsWith("ENTER")
                val latitude = parts[3].toDouble()
                val longitude = parts[4].toDouble()

                if (isEntered) {
                    repository.enterIntoStill(
                        timestamp, latitude, longitude
                    )
                } else {
                    repository.exitFromStill(
                        timestamp, latitude, longitude
                    )
                }

            } catch (e: Exception) {
            }
        }
        Log.d("Debug", "Complete to write Sedentary Events")

        resources.openRawResource(R.raw.missions).bufferedReader().readLines().forEach { line ->
            val parts = line.split(",").map { it.trim() }
            try {
                val prepareTime = parts[1].toLong()
                val standByTime = parts[2].toLong()
                val missionTime = parts[3].toLong()
                val reactionTime = parts[4].toLong()
                val latitude = parts[5].toDouble()
                val longitude = parts[6].toDouble()
                val state = parts[7]

                val id = if (prepareTime > 0) {
                    repository.prepareMission(
                        timestamp = prepareTime,
                        latitude = latitude,
                        longitude = longitude
                    )
                } else {
                    null
                }
                if (id != null && standByTime > 0) {
                    repository.standByMission(
                        timestamp = standByTime,
                        latitude = latitude,
                        longitude = longitude,
                        id = id
                    )
                }

                if (id != null && missionTime > 0) {
                    repository.startMission(
                        timestamp = missionTime,
                        latitude = latitude,
                        longitude = longitude,
                        id = id
                    )
                }

                if (id != null && reactionTime > 0) {
                    repository.completeMission(
                        timestamp = reactionTime,
                        latitude = latitude,
                        longitude = longitude,
                        id = id,
                        isSucceeded = state == "SUCCESS"
                    )
                }
            } catch (e: Exception) {
            }
        }
        Log.d("Debug", "Complete to write Missions")
    }
}
