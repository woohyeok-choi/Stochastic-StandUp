package kaist.iclab.standup.smi.ui.splash

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tedpark.tedpermission.rx2.TedRx2Permission
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.*
import kotlinx.coroutines.launch

class SplashViewModel(
    private val context: Context,
    private val permissions: Array<String>) : BaseViewModel<SplashNavigator>() {

    private fun tryLaunch(call: suspend () -> Unit) = viewModelScope.launch {
        try {
            call()
        } catch (e: Exception) {
            navigator?.navigateError(e)
        }
    }

    fun doSignIn(activity: Activity) = tryLaunch {
        if (context.checkNetworkConnection()) {
            GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(activity)
                .asSuspend(throwable = error(R.string.error_google_play_service_outdated))

            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(activity, options)

            ui { navigator?.navigateSignIn(client.signInIntent) }
        } else {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                ui { navigator?.navigateAuth(currentUser)}
            } else {
                ui { navigator?.navigateError(error(R.string.error_no_internect_connection)) }
            }
        }
    }

    fun doAuth(intent: Intent?) = tryLaunch {
        val account = GoogleSignIn.getSignedInAccountFromIntent(intent).asSuspend()
            ?: throwError(R.string.error_no_google_account)

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val user = FirebaseAuth.getInstance().signInWithCredential(credential).asSuspend()?.user
            ?: throwError(R.string.error_firebase_invalid_credential)

        ui { navigator?.navigateAuth(user) }
    }

    fun doPermission() = tryLaunch {
        val result = TedRx2Permission.with(context)
            .setRationaleTitle(R.string.dialog_permission_rationale_title)
            .setRationaleMessage(R.string.dialog_permission_rationale_text)
            .setDeniedTitle(R.string.dialog_permission_denied_title)
            .setDeniedMessage(R.string.dialog_permission_denied_text)
            .setPermissions(*permissions)
            .request().asSuspend()

        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${context.packageName}")
        }

        ui { navigator?.navigatePermission(result?.isGranted == true, intent) }
    }

    fun doPermissionAgain() = tryLaunch {
        val result = context.checkPermissions(permissions)
        if (!result) throwError(R.string.error_permission_denied)

        ui { navigator?.navigatePermissionAgain() }
    }

    @SuppressLint("BatteryLife")
    fun doWhitelist() = tryLaunch {
        if (context.checkWhitelist()) {
            ui { navigator?.navigateSuccess() }
        } else {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
            ui { navigator?.navigateWhitelist(intent) }
        }
    }

    fun doWhitelistAgain() = tryLaunch {
        if (context.checkWhitelist()) {
            ui { navigator?.navigateSuccess() }
        } else {
            throwError(R.string.error_whitelist_denied)
        }
    }
}