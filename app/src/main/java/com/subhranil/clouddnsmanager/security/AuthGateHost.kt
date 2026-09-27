package com.subhranil.clouddnsmanager.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.subhranil.clouddnsmanager.security.components.PinEntryDialog
import kotlinx.coroutines.CompletableDeferred
import org.koin.compose.koinInject

private class PinPrompt(val reason: String) {
    val result = CompletableDeferred<Boolean>()
}

/**
 * Renders the prompts requested through [AuthGate]. Place exactly once, near the root.
 *
 * - App PIN set: biometrics first (if enabled), with "Use PIN" / errors falling back to the PIN.
 * - No app PIN: the phone's own screen lock (biometric or device PIN) via the system prompt.
 * - No screen lock on the device at all: allowed — the caller's confirm dialog was the gate.
 */
@Composable
fun AuthGateHost(
    authGate: AuthGate = koinInject(),
    repository: AppLockRepository = koinInject(),
    biometrics: BiometricAuthenticator = koinInject(),
) {
    val activity = rememberFragmentActivity()
    var pinPrompt by remember { mutableStateOf<PinPrompt?>(null) }

    suspend fun askForPin(reason: String): Boolean {
        val prompt = PinPrompt(reason)
        pinPrompt = prompt
        return try {
            prompt.result.await()
        } finally {
            pinPrompt = null
        }
    }

    suspend fun authenticate(reason: String): Boolean {
        if (repository.isPinSetNow()) {
            if (repository.biometricsEnabledNow() && biometrics.canUseBiometrics()) {
                when (
                    biometrics.prompt(
                        activity = activity,
                        title = "Confirm it's you",
                        subtitle = reason,
                        allowDeviceCredential = false,
                        negativeText = "Use PIN",
                    )
                ) {
                    BiometricResult.Success -> {
                        repository.resetFailedAttempts()
                        return true
                    }
                    BiometricResult.Cancelled -> return false
                    BiometricResult.Fallback, is BiometricResult.Error -> Unit // fall through to PIN
                }
            }
            return askForPin(reason)
        }

        if (biometrics.canUseDeviceCredential()) {
            return biometrics.prompt(
                activity = activity,
                title = "Confirm it's you",
                subtitle = reason,
                allowDeviceCredential = true,
            ) == BiometricResult.Success
        }
        return true
    }

    LaunchedEffect(authGate) {
        for (request in authGate.requests) {
            if (request.result.isCompleted) continue // caller already gave up
            val granted = try {
                authenticate(request.reason)
            } catch (e: Exception) {
                request.result.complete(false)
                throw e
            }
            request.result.complete(granted)
        }
    }

    DisposableEffect(Unit) {
        onDispose { pinPrompt?.result?.complete(false) }
    }

    pinPrompt?.let { prompt ->
        PinEntryDialog(
            title = "Enter your PIN",
            message = prompt.reason,
            repository = repository,
            onVerified = { prompt.result.complete(true) },
            onDismiss = { prompt.result.complete(false) },
        )
    }
}
