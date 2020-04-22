package kaist.iclab.standup.smi

import android.Manifest
import android.os.Build
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationRequest
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.GeoApiContext
import kaist.iclab.standup.smi.repository.*
import kaist.iclab.standup.smi.tracker.ActivityTracker
import kaist.iclab.standup.smi.tracker.LocationTracker
import kaist.iclab.standup.smi.tracker.StepCountTracker
import kaist.iclab.standup.smi.ui.config.ConfigViewModel
import kaist.iclab.standup.smi.ui.dashboard.DashboardViewModel
import kaist.iclab.standup.smi.ui.main.MainViewModel
import kaist.iclab.standup.smi.ui.splash.SplashViewModel
import kaist.iclab.standup.smi.ui.timeline.TimelineViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private val DEFAULT_PERMISSIONS = arrayOf(
    Manifest.permission.INTERNET,
    Manifest.permission.RECEIVE_BOOT_COMPLETED,
    Manifest.permission.ACCESS_WIFI_STATE,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_NETWORK_STATE,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    Manifest.permission.VIBRATE
)

private val PERMISSIONS =
    when {
        Build.VERSION.SDK_INT == Build.VERSION_CODES.P -> DEFAULT_PERMISSIONS + arrayOf(
            Manifest.permission.FOREGROUND_SERVICE
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> DEFAULT_PERMISSIONS + arrayOf(
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        else -> DEFAULT_PERMISSIONS
    }
private val COLLECTION_ROOT = if (BuildConfig.FIREBASE_TEST_MODE) "tests" else "users"
private const val COLLECTION_EVENTS = "events"

private const val COLLECTION_MISSIONS = "missions"
private const val COLLECTION_PLACES = "places"
private const val COLLECTION_INTERACTIONS = "interactions"
private const val DOCUMENT_USER = "user"

private val FITNESS_OPTIONS = FitnessOptions.builder()
    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
    .build()

val firebaseModules = module {
    factory(named(COLLECTION_EVENTS)) {
        {
            val userName = FirebaseAuth.getInstance().currentUser?.email
            val instance = FirebaseFirestore.getInstance()

            userName?.let { name ->
                instance.collection(COLLECTION_ROOT).document(name).collection(COLLECTION_EVENTS)
            }
        }
    }

    factory(named(COLLECTION_MISSIONS)) {
        {
            val userName = FirebaseAuth.getInstance().currentUser?.email
            val instance = FirebaseFirestore.getInstance()

            userName?.let { name ->
                instance.collection(COLLECTION_ROOT).document(name).collection(COLLECTION_MISSIONS)
            }
        }
    }

    factory(named(COLLECTION_PLACES)) {
        {
            val userName = FirebaseAuth.getInstance().currentUser?.email
            val instance = FirebaseFirestore.getInstance()

            userName?.let { name ->
                instance.collection(COLLECTION_ROOT).document(name).collection(COLLECTION_PLACES)
            }
        }
    }

    factory(named(COLLECTION_INTERACTIONS)) {
        {
            val userName = FirebaseAuth.getInstance().currentUser?.email
            val instance = FirebaseFirestore.getInstance()
            userName?.let { name ->
                instance.collection(COLLECTION_ROOT).document(name)
                    .collection(COLLECTION_INTERACTIONS)
            }
        }
    }

    factory(named(DOCUMENT_USER)) {
        {
            val userName = FirebaseAuth.getInstance().currentUser?.email
            val instance = FirebaseFirestore.getInstance()
            userName?.let { name ->
                instance.collection(COLLECTION_ROOT).document(name)
            }
        }
    }
}

val trackerModule = module {
    single(createdAtStart = false) {
        ActivityTracker(
            context = androidContext(),
            request = ActivityTransitionRequest(
                listOf(
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build(),
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
                )
            ),
            pendingIntent = StandUpIntentService.intentForActivity(androidContext())
        )
    }

    single(createdAtStart = false) {
        LocationTracker(
            context = androidContext(),
            request = LocationRequest.create()
                .setInterval(TimeUnit.MINUTES.toMillis(3))
                .setSmallestDisplacement(10.0F)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
            pendingIntent = StandUpIntentService.intentForLocation(androidContext())
        )
    }

    single(createdAtStart = false) {
        StepCountTracker(
            context = androidContext(),
            fitnessOptions = FITNESS_OPTIONS,
            pendingIntent = StandUpIntentService.intentForActivity(androidContext())
        )
    }
}

val repositoryModules = module {
    single(createdAtStart = false) {
        EventRepository(
            reference = get(named(COLLECTION_EVENTS))
        )
    }

    single(createdAtStart = false) {
        MissionRepository(
            reference = get(named(COLLECTION_MISSIONS))
        )
    }

    single(createdAtStart = false) {
        StatRepository(
            geoApiContext = GeoApiContext.Builder()
                .apiKey(androidContext().getString(R.string.google_api_key))
                .connectTimeout(10, TimeUnit.SECONDS)
                .maxRetries(5)
                .readTimeout(10, TimeUnit.SECONDS)
                .build(),
            placesClient = Places.createClient(androidContext()),
            rootReference = get(named(DOCUMENT_USER)),
            docReference = get(named(COLLECTION_PLACES))
        )
    }

    single<IncentiveRepository>(createdAtStart = false) {
        StochasticIncentiveRepository()
    }

    single(createdAtStart = false) {
        StandUpMissionHandler(
            missionRepository = get(),
            statRepository = get(),
            eventRepository = get(),
            incentiveRepository = get()
        )
    }
}

val viewModelModules = module {
    viewModel {
        SplashViewModel(androidContext(), PERMISSIONS, FITNESS_OPTIONS)
    }

    viewModel {
        MainViewModel()
    }

    viewModel {
        TimelineViewModel(
            eventRepository = get(),
            missionRepository = get(),
            statRepository = get(),
            placeReference = get(named(COLLECTION_PLACES))
        )
    }

    viewModel {
        DashboardViewModel(
            context = androidContext(),
            eventRepository = get(),
            missionRepository = get()
        )
    }

    viewModel {
        ConfigViewModel(
            context = androidContext(),
            permissions = PERMISSIONS
        )
    }
}