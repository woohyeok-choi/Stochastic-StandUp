package kaist.iclab.standup.smi

import android.app.Application
import com.google.android.libraries.maps.MapsInitializer
import com.google.android.libraries.places.api.Places
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import github.agustarc.koap.Koap
import kaist.iclab.standup.smi.common.Notifications
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import net.danlew.android.joda.JodaTimeAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.*

class StandUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Koap.bind(this, LocalPrefs, RemotePrefs)
        RemotePrefs.localMode = BuildConfig.IS_LOCAL_CONFIG
        JodaTimeAndroid.init(this)
        Places.initialize(this, getString(R.string.google_api_key), Locale.getDefault())
        MapsInitializer.initialize(this)
        Notifications.bind(this)
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder().apply {
                isPersistenceEnabled = true
            }.build()

        startKoin {
            androidContext(this@StandUpApplication)

            modules(listOf(
                firebaseModules,
                trackerModule,
                repositoryModules,
                viewModelModules
            ))
        }
    }
}