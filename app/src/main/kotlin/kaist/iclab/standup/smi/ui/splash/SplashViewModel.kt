package kaist.iclab.standup.smi.ui.splash

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tedpark.tedpermission.rx2.TedRx2Permission
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.*

class SplashViewModel(
    private val context: Context,
    private val permissions: Array<String>,
    navigator: SplashNavigator) : BaseViewModel<SplashNavigator>(navigator) {
    override suspend fun onLoad(extras: Bundle?) {}

    fun doSignIn(activity: Activity) = tryLaunch {
        GoogleApiAvailability.getInstance()
            .makeGooglePlayServicesAvailable(activity)
            .asSuspend(throwable = error(R.string.error_google_play_service_outdated))

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(activity, options)

        ui { nav?.navigateSignIn(client.signInIntent) }
    }

    fun doAuth(intent: Intent?) = tryLaunch {
        val account = GoogleSignIn.getSignedInAccountFromIntent(intent).asSuspend()
            ?: throwError(R.string.error_no_google_account)
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val user = FirebaseAuth.getInstance().signInWithCredential(credential).asSuspend()?.user
            ?: throwError(R.string.error_firebase_invalid_credential)
        Log.d("ZXCV", "${account.photoUrl}")
        ui { nav?.navigateAuth(user) }
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

        ui { nav?.navigatePermission(result?.isGranted == true, intent) }
    }

    fun doPermissionAgain() = tryLaunch {
        val result = context.checkPermissions(permissions)
        if (!result) throwError(R.string.error_permission_denied)

        ui { nav?.navigatePermissionAgain() }
    }

    @SuppressLint("BatteryLife")
    fun doWhitelist() = tryLaunch {
        if (context.checkWhitelist()) {
            ui { nav?.navigateSuccess() }
        } else {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
            ui { nav?.navigateWhitelist(intent) }
        }
    }

    fun doWhitelistAgain() = tryLaunch {
        if (context.checkWhitelist()) {
            ui { nav?.navigateSuccess() }
        } else {
            throwError(R.string.error_whitelist_denied)
        }
    }
}