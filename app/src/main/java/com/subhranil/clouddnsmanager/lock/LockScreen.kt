package com.subhranil.clouddnsmanager.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.security.BiometricAuthenticator
import com.subhranil.clouddnsmanager.security.BiometricResult
import com.subhranil.clouddnsmanager.security.components.PinInputField
import com.subhranil.clouddnsmanager.security.components.rememberLockoutSecondsLeft
import com.subhranil.clouddnsmanager.security.rememberFragmentActivity
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Full-screen overlay shown over the app while it is locked. */
@Composable
fun LockScreen(
    modifier: Modifier = Modifier,
    viewModel: LockViewModel = koinViewModel(),
    biometrics: BiometricAuthenticator = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = rememberFragmentActivity()
    val scope = rememberCoroutineScope()
    val canUseBiometrics = state.biometricsEnabled && biometrics.canUseBiometrics()

    fun promptBiometrics() {
        scope.launch {
            val result = biometrics.prompt(
                activity = activity,
                title = "Unlock Dns Manager",
                subtitle = null,
                allowDeviceCredential = false,
                negativeText = "Use PIN",
            )
            if (result == BiometricResult.Success) viewModel.onAction(LockScreenIntent.BiometricSucceeded)
        }
    }

    // Offer biometrics straight away when the lock screen appears
    LaunchedEffect(canUseBiometrics) {
        if (canUseBiometrics) promptBiometrics()
    }

    // Never let Back reveal the screens underneath
    BackHandler { activity.moveTaskToBack(true) }

    if (state.showForgotPinConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(LockScreenIntent.DismissForgotPin) },
            title = { Text("Forgot your PIN?") },
            text = {
                Text("You'll be logged out. Your saved API token and PIN are removed from this device, and you'll need to enter the token again.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(LockScreenIntent.ConfirmForgotPin) }) {
                    Text("Log out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(LockScreenIntent.DismissForgotPin) }) { Text("Cancel") }
            },
        )
    }

    LockScreenContent(
        state = state,
        onAction = viewModel::onAction,
        canUseBiometrics = canUseBiometrics,
        onUseBiometrics = ::promptBiometrics,
        modifier = modifier,
    )
}

/**
 * Stateless lock screen (also used by previews). The biometric prompt needs the hosting
 * Activity, so it stays in [LockScreen] and arrives here as [onUseBiometrics].
 */
@Composable
fun LockScreenContent(
    state: LockScreenState,
    onAction: (LockScreenIntent) -> Unit,
    canUseBiometrics: Boolean,
    onUseBiometrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val secondsLeft = rememberLockoutSecondsLeft(state.lockedOutUntilMs)
    // An opaque Surface also swallows touches meant for the content below
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Dns Manager is locked",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your PIN to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            PinInputField(
                value = state.pin,
                onValueChange = { onAction(LockScreenIntent.UpdatePin(it)) },
                enabled = secondsLeft == 0L && !state.verifying,
                isError = state.error != null,
                autoFocus = !canUseBiometrics,
            )

            val status = if (secondsLeft > 0) "Too many attempts. Try again in ${secondsLeft}s." else state.error
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    status,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }

            if (canUseBiometrics) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onUseBiometrics) { Text("Use biometrics") }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { onAction(LockScreenIntent.ForgotPin) }) {
                Text("Forgot PIN? Log out")
            }
        }
    }
}
