package kaist.iclab.standup.smi.ui.splash

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.FitnessOptions
import com.google.firebase.auth.FirebaseUser
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseActivity
import kaist.iclab.standup.smi.common.toast
import kaist.iclab.standup.smi.databinding.ActivitySplashBinding
import kaist.iclab.standup.smi.ui.main.MainActivity
import org.koin.androidx.viewmodel.ext.android.viewModel


class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>(), SplashNavigator {
    override val viewModel: SplashViewModel by viewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.activity_splash

    override fun beforeExecutePendingBindings() {
        viewModel.navigator = this
        viewModel.doSignIn(this)
    }

    override fun navigateSignIn(intent: Intent) {
        startActivityForResult(intent, REQUEST_CODE_GOOGLE_SIGN_IN)
    }

    override fun navigateFitnessAuth(account: GoogleSignInAccount, option: FitnessOptions) {
        GoogleSignIn.requestPermissions(this, REQUEST_CODE_GOOGLE_FITNESS, account, option)
    }

    override fun navigateAuth() {
        viewModel.doPermission()
    }

    override fun navigatePermission(isGranted: Boolean, intent: Intent) {
        if (isGranted) {
            viewModel.doWhitelist()
        } else {
            startActivityForResult(intent, REQUEST_CODE_PERMISSION_SETTING)
        }
    }

    override fun navigatePermissionAgain() {
        viewModel.doWhitelist()
    }

    override fun navigateWhitelist(intent: Intent) {
        startActivityForResult(intent, REQUEST_CODE_WHITE_LIST)
    }

    override fun navigateSuccess() {
        SystemClock.sleep(1000)
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun navigateError(throwable: Throwable?) {
        toast(throwable, isShort = false)
        SystemClock.sleep(1000)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_GOOGLE_SIGN_IN -> viewModel.doAuth(data)
            REQUEST_CODE_GOOGLE_FITNESS -> {
                if (resultCode == Activity.RESULT_OK) viewModel.doPermission()
            }
            REQUEST_CODE_PERMISSION_SETTING -> viewModel.doPermissionAgain()
            REQUEST_CODE_WHITE_LIST -> viewModel.doWhitelistAgain()
        }
    }

    companion object {
        const val REQUEST_CODE_PERMISSION_SETTING = 0x09
        const val REQUEST_CODE_WHITE_LIST = 0x10
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 0x11
        const val REQUEST_CODE_GOOGLE_FITNESS = 0x12
    }
}