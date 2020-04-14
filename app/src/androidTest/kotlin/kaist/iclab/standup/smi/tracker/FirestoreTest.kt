package kaist.iclab.standup.smi.tracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.libraries.places.api.Places
import com.google.common.truth.Truth.assertThat
import com.google.maps.GeoApiContext
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.repository.StatRepository
import kotlinx.coroutines.runBlocking
import net.danlew.android.joda.JodaTimeAndroid
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class FirestoreTest {
    private lateinit var statRepository: StatRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fireStore = initializeFirestore()

        JodaTimeAndroid.init(context)
        statRepository = StatRepository(
            geoApiContext = GeoApiContext.Builder()
                .apiKey(context.getString(R.string.google_api_key))
                .connectTimeout(10, TimeUnit.SECONDS)
                .maxRetries(5)
                .readTimeout(10, TimeUnit.SECONDS)
                .build(),
            placesClient = Places.createClient(context),
            rootReference = { fireStore.collection("tests").document("test") },
            docReference =  { fireStore.collection("tests").document("test").collection("place") }
        )
    }

    @Test
    fun testNewPlace() {
        runBlocking {
            statRepository.updateVisitEvent(30.0, 130.0, System.currentTimeMillis())
            val placeStat = statRepository.getPlaceStat(30.0, 130.0)
            val overallStat = statRepository.getOverallStat()

            assertThat(placeStat).isNotNull()
            assertThat(placeStat?.numVisit).isEqualTo(1)
            assertThat(overallStat?.numPlaces).isEqualTo(1)
            assertThat(overallStat?.numVisit).isEqualTo(1)
        }
    }
}

