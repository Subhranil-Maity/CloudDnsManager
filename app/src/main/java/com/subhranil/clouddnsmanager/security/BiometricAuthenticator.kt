package com.subhranil.clouddnsmanager.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface BiometricResult {
    data object Success : BiometricResult
    /** The user tapped the negative button (e.g. "Use PIN"). */
    data object Fallback : BiometricResult
    data object Cancelled : BiometricResult
    /** Hardware / too-many-attempts errors — callers should fall back to the PIN. */
    data class Error(val message: String) : BiometricResult
}

/** Thin coroutine wrapper around [BiometricPrompt]. */
class BiometricAuthenticator(private val context: Context) {

    fun canUseBiometrics(): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /** True when the phone has a screen lock (biometric or PIN/pattern/password). */
    fun canUseDeviceCredential(): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Show the system prompt. With [allowDeviceCredential] the phone's own PIN/pattern is
     * offered and no negative button is shown; otherwise [negativeText] is the fallback button.
     */
    suspend fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String?,
        allowDeviceCredential: Boolean,
        negativeText: String = "Cancel",
    ): BiometricResult = suspendCancellableCoroutine { cont ->
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!cont.isActive) return
                cont.resume(
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricResult.Fallback
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> BiometricResult.Cancelled
                        else -> BiometricResult.Error(errString.toString())
                    }
                )
            }
            // onAuthenticationFailed (non-matching finger) keeps the prompt open — nothing to do.
        }

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val builder = BiometricPrompt.PromptInfo.Builder().setTitle(title)
        if (subtitle != null) builder.setSubtitle(subtitle)
        if (allowDeviceCredential) {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            builder.setNegativeButtonText(negativeText)
        }

        prompt.authenticate(builder.build())
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}

/** The hosting [FragmentActivity] (MainActivity), needed to show a [BiometricPrompt]. */
@Composable
fun rememberFragmentActivity(): FragmentActivity {
    var ctx: Context = LocalContext.current
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    error("BiometricPrompt requires the app to be hosted in a FragmentActivity")
}
