package kaist.iclab.standup.smi.ui.splash

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.FitnessOptions
import com.google.firebase.auth.FirebaseUser
import kaist.iclab.standup.smi.base.BaseNavigator

interface SplashNavigator : BaseNavigator {
    fun navigateSignIn(intent: Intent)
    fun navigateAuth()
    fun navigateFitnessAuth(account: GoogleSignInAccount, option: FitnessOptions)
    fun navigatePermission(isGranted: Boolean, intent: Intent)
    fun navigatePermissionAgain()
    fun navigateWhitelist(intent: Intent)
    fun navigateSuccess()
    fun navigateError(throwable: Throwable?)
}