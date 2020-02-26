package kaist.iclab.standup.smi.ui.main

import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kaist.iclab.standup.smi.base.BaseNavigator
import kaist.iclab.standup.smi.base.BaseViewModel

class MainViewModel(navigator: MainNavigator) : BaseViewModel<MainNavigator>(navigator) {
    override suspend fun onLoad(extras: Bundle?) {
        val crash = FirebaseCrashlytics.getInstance()

        FirebaseAuth.getInstance().currentUser?.email?.let { crash.setUserId(it) }
        crash.sendUnsentReports()


    }
}