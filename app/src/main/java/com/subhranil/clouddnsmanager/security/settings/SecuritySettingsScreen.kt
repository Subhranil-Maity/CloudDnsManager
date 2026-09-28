package com.subhranil.clouddnsmanager.security.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.security.BiometricAuthenticator
import com.subhranil.clouddnsmanager.security.BiometricResult
import com.subhranil.clouddnsmanager.security.rememberFragmentActivity
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecuritySettingsViewModel = koinViewModel(),
    biometrics: BiometricAuthenticator = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = rememberFragmentActivity()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler { viewModel.onAction(SecuritySettingsIntent.Back) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(SecuritySettingsIntent.MessageShown)
        }
    }

    if (state.showRemovePinConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(SecuritySettingsIntent.DismissRemovePin) },
            title = { Text("Remove PIN?") },
            text = { Text("The app will no longer lock, and biometric unlock will be turned off.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(SecuritySettingsIntent.ConfirmRemovePin) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(SecuritySettingsIntent.DismissRemovePin) }) { Text("Cancel") }
            },
        )
    }

    SecuritySettingsContent(
        state = state,
        onAction = viewModel::onAction,
        onEnableBiometrics = {
            // Prove the biometric works before relying on it
            scope.launch {
                val result = biometrics.prompt(
                    activity = activity,
                    title = "Enable biometric unlock",
                    subtitle = "Confirm with your fingerprint or face",
                    allowDeviceCredential = false,
                )
                if (result == BiometricResult.Success) {
                    viewModel.onAction(SecuritySettingsIntent.EnableBiometricsVerified)
                }
            }
        },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Stateless security settings (also used by previews). Enabling biometrics needs the hosting
 * Activity for the prompt, so that stays in [SecuritySettingsScreen] as [onEnableBiometrics].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsContent(
    state: SecuritySettingsState,
    onAction: (SecuritySettingsIntent) -> Unit,
    onEnableBiometrics: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SecuritySettingsIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("App PIN", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.isPinSet)
                    "The app locks on launch and after a minute in the background. Logging out and other changes need your PIN or biometrics."
                else
                    "Set a PIN to lock the app on launch and after a minute in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (state.isPinSet) {
                Row {
                    OutlinedButton(onClick = { onAction(SecuritySettingsIntent.ChangePin) }) {
                        Text("Change PIN")
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { onAction(SecuritySettingsIntent.RemovePin) }) {
                        Text("Remove PIN", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Button(onClick = { onAction(SecuritySettingsIntent.SetPin) }) { Text("Set PIN") }
            }

            if (state.isPinSet && state.biometricsAvailable) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Unlock with biometrics", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Use your fingerprint or face instead of typing the PIN. The PIN still works as a fallback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = state.biometricsEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                onEnableBiometrics()
                            } else {
                                onAction(SecuritySettingsIntent.DisableBiometrics)
                            }
                        },
                    )
                }
            }
        }
    }
}
