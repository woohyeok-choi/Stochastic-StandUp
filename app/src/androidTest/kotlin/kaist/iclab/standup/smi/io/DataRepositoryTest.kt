package kaist.iclab.standup.smi.io

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kaist.iclab.standup.smi.common.asSuspend
import kaist.iclab.standup.smi.io.data.Event
import kotlinx.coroutines.runBlocking
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.junit.Before
import org.junit.Test
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DataRepositoryTest {
    private val host = "10.0.2.2:8080"
    private lateinit var reference: DocumentReference
    private lateinit var repository: DataRepository

    @Before
    fun setUp() {
        Firebase.initialize(ApplicationProvider.getApplicationContext())
        JodaTimeAndroid.init(ApplicationProvider.getApplicationContext())

        val setting = FirebaseFirestoreSettings.Builder()
            .setHost(host)
            .setSslEnabled(false)
            .setPersistenceEnabled(false)
            .build()

        val firestore = FirebaseFirestore.getInstance()

        firestore.firestoreSettings = setting
        reference = firestore.collection("tests").document("test")
        repository = DataRepository { reference }
    }

    @Test
    fun dataRepository_test1_fillDataAndFindLatestIncompleteEvent() {
        val dateTimes = listOf(
            "2020-02-01T13:50:00Z",
            "2020-02-01T16:20:00Z",
            "2020-02-01T17:50:00Z",
            "2020-02-01T19:20:00Z",
            "2020-02-01T23:20:00Z"
        )
        runBlocking {
            (dateTimes.indices).forEach { index ->
                if (index > 0) {
                    repository.endEvent(reference, LocalDateTime.parse(dateTimes[index]).toDate().time)
                } else {
                    repository.beginEvent(reference, Event.TYPE_VEHICLE, LocalDateTime(dateTimes[index]).toDate().time)
                }
            }
            var event = repository.getLatestIncompleteEvent(reference)
            assertThat(event).isNotNull()
            event = event!!

            assertThat(event.endTime).isNull()
            assertThat(event.startTime).isEqualTo(LocalDateTime.parse(dateTimes.last()).toDate().time)
        }
    }

    @Test
    fun dataRepository_fillDataAndFindLatestIncompleteEvent() {
        val dateTimes = listOf(
            "2020-02-01T13:50:00Z",
            "2020-02-01T16:20:00Z",
            "2020-02-01T17:50:00Z",
            "2020-02-01T19:20:00Z",
            "2020-02-01T23:20:00Z"
        )
        runBlocking {
            (dateTimes.indices).forEach { index ->
                if (index > 0) {
                    repository.endEvent(reference, LocalDateTime.parse(dateTimes[index]).toDate().time)
                } else {
                    repository.beginEvent(reference, Event.TYPE_VEHICLE, LocalDateTime(dateTimes[index]).toDate().time)
                }
            }
            var event = repository.getLatestIncompleteEvent(reference)
            assertThat(event).isNotNull()
            event = event!!

            assertThat(event.endTime).isNull()
            assertThat(event.startTime).isEqualTo(LocalDateTime.parse(dateTimes.last()).toDate().time)
        }
    }
}