package kaist.iclab.standup.smi.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.github.kittinunf.fuel.core.FuelError
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kaist.iclab.standup.smi.R

class StandUpError(
    @get:StringRes val res: Int,
    override val message: String? = null
): Exception(message) {
    fun toString(context: Context) : String = listOfNotNull(context.getString(res), message).joinToString(" - ")
    fun toString(fragment: Fragment) : String = listOfNotNull(fragment.getString(res), message).joinToString(" - ")
}

private fun googleApiCodeToMessage(code: Int) = when (code) {
    CommonStatusCodes.API_NOT_CONNECTED -> "Api is not connected."
    CommonStatusCodes.CANCELED -> "Request is canceled."
    CommonStatusCodes.DEVELOPER_ERROR -> "Api is incorrectly configured."
    CommonStatusCodes.ERROR -> "Unknown error occurs."
    CommonStatusCodes.INTERNAL_ERROR -> "Internal error occurs"
    CommonStatusCodes.INTERRUPTED -> "Request is interrupted."
    CommonStatusCodes.INVALID_ACCOUNT -> "Invalid account name."
    CommonStatusCodes.NETWORK_ERROR -> "Network error occurs."
    CommonStatusCodes.RESOLUTION_REQUIRED -> "Resolution is required."
    CommonStatusCodes.SIGN_IN_REQUIRED -> "Sign-in is required."
    CommonStatusCodes.TIMEOUT -> "Request is timed out."
    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign-in canceled."
    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign-in already in progress."
    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign-in failed. There may be no Google account connected to this device, or Google Play service is outdated."
    else -> null
}

fun throwError(res: Int, message: String? = null): Nothing = throw StandUpError(res, message)

fun error(res: Int, message: String? = null) = StandUpError(res, message)

fun Throwable?.wrap() : StandUpError = when(this) {
    is StandUpError -> this
    is ApiException -> StandUpError(R.string.error_google_api, googleApiCodeToMessage(statusCode))
    is FirebaseAuthInvalidUserException -> StandUpError(R.string.error_firebase_invalid_user)
    is FirebaseAuthInvalidCredentialsException -> StandUpError(R.string.error_firebase_invalid_credential)
    is FirebaseAuthUserCollisionException -> StandUpError(R.string.error_firebase_user_collision)
    is FuelError -> StandUpError(R.string.error_http_connection, message)
    else -> StandUpError(R.string.error_general, this?.message)
}