package com.subhranil.clouddnsmanager.selectzones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.selectzones.components.ZonesScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectZoneScreen(
    modifier: Modifier = Modifier,
    viewModel: SelectZoneViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Logging out deletes the saved token, so require an explicit confirmation
    if (state.showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(SelectZoneIntent.DismissLogout) },
            title = { Text("Log out?") },
            text = { Text("Your saved Cloudflare API token will be removed from this device. You'll need to enter it again to sign back in.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(SelectZoneIntent.ConfirmLogout) }) {
                    Text("Log out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(SelectZoneIntent.DismissLogout) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Exhaustively branching the UI based directly on the sealed dataState
    when (val dataState = state.dataState) {
        is SelectZoneDataState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is SelectZoneDataState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Error icon",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = dataState.message,
                    style =
                        MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { viewModel.onAction(SelectZoneIntent.Retry) }
                ) {
                    Text(text = "Retry")
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Escape hatch when the saved token has been revoked or lost its permissions
                TextButton(
                    onClick = { viewModel.onAction(SelectZoneIntent.RequestLogout) }
                ) {
                    Text(text = "Log out")
                }
            }
        }

        is SelectZoneDataState.ZoneData -> {
            ZonesScreen(
                zones = dataState.zones,
                onZoneClick = { zone ->
                    viewModel.onAction(SelectZoneIntent.SelectZone(zone.id, zone.name, zone.account.id))
                },
                onLogout = { viewModel.onAction(SelectZoneIntent.RequestLogout) },
                onOpenSecurity = { viewModel.onAction(SelectZoneIntent.OpenSecurity) },
                modifier = modifier.fillMaxSize(),
                isLoading = false // Handled natively by our top-level state branching now
            )
        }
    }
}