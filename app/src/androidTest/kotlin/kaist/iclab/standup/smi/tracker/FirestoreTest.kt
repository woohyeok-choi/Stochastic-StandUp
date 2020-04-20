package kaist.iclab.standup.smi.tracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import kaist.iclab.standup.smi.Debug
import kaist.iclab.standup.smi.StandUpMissionHandler
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.repository.*
import kotlinx.coroutines.runBlocking
import net.danlew.android.joda.JodaTimeAndroid
import org.junit.Before
import org.junit.Test

class FirestoreTest {
    private lateinit var statRepository: StatRepository
    private lateinit var missionRepository: MissionRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var incentiveRepository: IncentiveRepository
    private lateinit var standUpMissionHandler: StandUpMissionHandler
    private lateinit var fireStore: FirebaseFirestore
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fireStore = initializeFirestore()

        JodaTimeAndroid.init(context)

        statRepository = StatRepository(
            rootReference = { fireStore.collection("tests").document("test") },
            docReference =  { fireStore.collection("tests").document("test").collection("places") }
        )

        eventRepository = EventRepository {
            fireStore.collection("tests").document("test").collection("events")
        }

        missionRepository = MissionRepository {
            fireStore.collection("tests").document("test").collection("missions")
        }

        incentiveRepository = StochasticIncentiveRepository()

        standUpMissionHandler = StandUpMissionHandler(
            missionRepository = missionRepository,
            eventRepository = eventRepository,
            statRepository = statRepository,
            incentiveRepository = incentiveRepository
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

    @Test
    fun testMissions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        RemotePrefs.incentiveMode = RemotePrefs.INCENTIVE_MODE_STOCHASTIC
        runBlocking {
            Debug.generateDummyMissions(
                repository = standUpMissionHandler,
                resources = context.resources
            )
            missionRepository.getMissions(0, Long.MAX_VALUE).forEach {
                println("${it}")
            }
        }
    }
}

