package kaist.iclab.standup.smi.io

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kaist.iclab.standup.smi.io.data.Event
import kaist.iclab.standup.smi.io.data.Events
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test


class SedentaryHistoryTest {
    private val host = "10.0.2.2:8080"
    private lateinit var reference: DocumentReference

    @Before
    fun setUp() {
        Firebase.initialize(ApplicationProvider.getApplicationContext())

        val setting = FirebaseFirestoreSettings.Builder()
            .setHost(host)
            .setSslEnabled(false)
            .setPersistenceEnabled(false)
            .build()

        val firestore = FirebaseFirestore.getInstance()

        firestore.firestoreSettings = setting
        reference = firestore.collection("tests").document("test")
    }

    @Test
    fun sedentaryHistory_createAndUpdate() {
        runBlocking {
            val defaultType = Event.TYPE_SEDENTARY
            val defaultStartTime = 10L
            val defaultEndTime = 15L
            val defaultLatitude = 15.12345
            val defaultLongitude = 35.5432

            val id = Event.create(reference) {
                type = defaultType
                startTime = defaultStartTime
                endTime = defaultEndTime
                latitude = defaultLatitude
                longitude = defaultLongitude
            }

            assertThat(id).isNotNull()

            var history = Event.get(reference, id!!)

            assertThat(history).isNotNull()

            history = history!!

            assertThat(history.id).isEqualTo(id)
            assertThat(history.startTime).isEqualTo(defaultStartTime)
            assertThat(history.endTime).isEqualTo(defaultEndTime)
            assertThat(history.latitude).isEqualTo(defaultLatitude)
            assertThat(history.longitude).isEqualTo(defaultLongitude)

            val updateEndTime = 100L
            val updateLatitude = 12.3245

            Event.update(reference, id) {
                endTime = updateEndTime
                latitude = updateLatitude
            }

            var updatedHistory = Event.get(reference, id)

            assertThat(updatedHistory).isNotNull()

            updatedHistory = updatedHistory!!

            assertThat(updatedHistory.startTime).isEqualTo(defaultStartTime)
            assertThat(updatedHistory.endTime).isEqualTo(updateEndTime)
            assertThat(updatedHistory.latitude).isEqualTo(updateLatitude)
            assertThat(updatedHistory.longitude).isEqualTo(defaultStartTime)
        }
    }

    @Test
    fun sedentaryHistory_bulkCreateAndRead() {
        val sizeOfReads = 50L
        runBlocking {
            (0 until 100).forEach {
                Event.create(reference) {
                    type = Event.TYPE_SEDENTARY
                    startTime = it.toLong()
                    endTime = it.toLong() + 1
                    latitude = it.toDouble()
                    longitude = it.toDouble()
                }
            }

            Event.select(reference) {
                Events.startTime greaterThanOrEqualTo 0
                Events.startTime lessThan sizeOfReads
            }.also {
                assertThat(it).hasSize(50)
                assertThat(it.all { h -> h.startTime!! in 0..49 }).isTrue()
            }
        }
    }
}