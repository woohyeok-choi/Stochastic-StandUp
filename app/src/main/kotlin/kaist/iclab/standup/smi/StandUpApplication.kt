package kaist.iclab.standup.smi

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import github.agustarc.koap.Koap
import kaist.iclab.standup.smi.io.pref.DebugPrefs
import kaist.iclab.standup.smi.io.pref.LocalPrefs
import kaist.iclab.standup.smi.io.pref.RemotePrefs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StandUpApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@StandUpApplication)
            modules(listOf(ioModule, viewModelModules))
        }

        Koap.bind(this, LocalPrefs, DebugPrefs)
        GlobalScope.launch {
            RemotePrefs.bind()
        }
    }
}