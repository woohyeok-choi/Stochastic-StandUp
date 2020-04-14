package kaist.iclab.standup.smi.repository

import com.google.firebase.firestore.CollectionReference
import kaist.iclab.standup.smi.data.Interaction
import kaist.iclab.standup.smi.data.Interactions
import java.util.*

class InteractionRepository(
    private val reference: () -> CollectionReference?
) {

    suspend fun getInteractions(fromTime: Long, toTime: Long, type: String? = null) = reference.invoke()?.let { reference ->
        Interaction.select(reference) {
            Interactions.timestamp greaterThanOrEqualTo fromTime
            Interactions.timestamp lessThan toTime
            if (!type.isNullOrBlank()) Interactions.type equalTo type
        }
    }

    suspend fun createInteraction(timestamp: Long, type: String, extras: String? = null): String? = reference.invoke()?.let { reference ->
        Interaction.create(reference) {
            this.offsetMs = TimeZone.getDefault().rawOffset
            this.timestamp = timestamp
            this.type = type
            this.extras = extras ?: ""
        }
    }
}