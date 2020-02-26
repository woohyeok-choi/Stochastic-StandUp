package kaist.iclab.standup.smi

import android.Manifest
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kaist.iclab.standup.smi.io.SedentaryTrackRepository
import kaist.iclab.standup.smi.ui.config.ConfigNavigator
import kaist.iclab.standup.smi.ui.config.ConfigViewModel
import kaist.iclab.standup.smi.ui.dashboard.DashboardNavigator
import kaist.iclab.standup.smi.ui.dashboard.DashboardViewModel
import kaist.iclab.standup.smi.ui.main.MainNavigator
import kaist.iclab.standup.smi.ui.main.MainViewModel
import kaist.iclab.standup.smi.ui.splash.SplashNavigator
import kaist.iclab.standup.smi.ui.splash.SplashViewModel
import kaist.iclab.standup.smi.ui.timeline.TimelineNavigator
import kaist.iclab.standup.smi.ui.timeline.TimelineViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val PERMISSIONS = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
    arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
} else {
    arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
}

private val ref: () -> DocumentReference? = {
    val userName = FirebaseAuth.getInstance().currentUser?.email
    val instance = FirebaseFirestore.getInstance()
    userName?.let { instance.collection("users").document(it) }
}

val ioModule = module {
    factory { ref }
    single { SedentaryTrackRepository(androidContext(), get())  }
}

val viewModelModules = module {
    viewModel { (navigator: SplashNavigator) ->
        SplashViewModel(androidContext(), PERMISSIONS, navigator)
    }

    viewModel { (navigator: MainNavigator) ->
        MainViewModel(navigator)
    }

    viewModel { (navigator: TimelineNavigator) ->
        TimelineViewModel(navigator)
    }

    viewModel { (navigator: DashboardNavigator) ->
        DashboardViewModel(navigator)
    }

    viewModel { (navigator: ConfigNavigator) ->
        ConfigViewModel(navigator)
    }
}