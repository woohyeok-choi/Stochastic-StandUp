package kaist.iclab.standup.smi.ui.splash

import android.content.Intent
import com.google.firebase.auth.FirebaseUser
import kaist.iclab.standup.smi.base.BaseNavigator

interface SplashNavigator : BaseNavigator {
    fun navigateSignIn(intent: Intent)
    fun navigateAuth(user: FirebaseUser)
    fun navigatePermission(isGranted: Boolean, intent: Intent)
    fun navigatePermissionAgain()
    fun navigateWhitelist(intent: Intent)
    fun navigateSuccess()
}