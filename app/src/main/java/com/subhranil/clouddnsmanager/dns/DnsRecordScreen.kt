package com.subhranil.clouddnsmanager.dns

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.dns.components.DnsRecordDetailDrawer
import com.subhranil.clouddnsmanager.dns.components.DnsRecordsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun DnsRecordScreen(
    zoneId: String,
    modifier: Modifier = Modifier,
    viewModel: DnsRecordViewModel = koinViewModel { parametersOf(zoneId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // intercept system back handler patterns cleanly
    BackHandler {
        viewModel.onAction(DnsRecordIntent.GoBack)
    }

    val selected = state.selectedItem

    // One-off messages go to the snackbar. While the detail sheet is open they're shown
    // inside it instead, because the modal sheet would cover the snackbar.
    LaunchedEffect(state.message, selected == null) {
        val message = state.message ?: return@LaunchedEffect
        if (selected != null) return@LaunchedEffect
        viewModel.onAction(DnsRecordIntent.ConsumeMessage)
        snackbarHostState.showSnackbar(message)
    }

    // Deleting can't be undone, so it's confirmed here; the ViewModel then asks the AuthGate
    if (state.showDeleteConfirmation && selected != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(DnsRecordIntent.DismissDelete) },
            title = { Text("Delete record?") },
            text = {
                Text(
                    "The ${selected.record.type.name} record ${selected.record.name} will be deleted from Cloudflare. " +
                        "This can't be undone. Its private note on this device is removed too."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(DnsRecordIntent.ConfirmDelete) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(DnsRecordIntent.DismissDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dynamic Content State Resolution ---
    when (val dataState = state.dnsRecordDataState) {
        is DnsRecordDataState.Error -> {
            // Clean, dedicated full screen error layout
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
                    text = dataState.error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { viewModel.onAction(DnsRecordIntent.Retry) }
                ) {
                    Text(text = "Retry")
                }
            }
        }

        // Loading passes an empty list to activate the Crossfade shimmer placeholder rows
        is DnsRecordDataState.Loading, is DnsRecordDataState.DnsRecordData -> {
            DnsRecordsScreen(
                dnsRecords = (dataState as? DnsRecordDataState.DnsRecordData)?.dnsList.orEmpty(),
                isLoading = dataState is DnsRecordDataState.Loading,
                refreshing = state.refreshing,
                snackbarHostState = snackbarHostState,
                onSelectRecord = { item -> viewModel.onAction(DnsRecordIntent.ShowDetailed(item.record.id)) },
                onAddRecord = { viewModel.onAction(DnsRecordIntent.AddRecord) },
                onRefresh = { viewModel.onAction(DnsRecordIntent.Refresh) },
                onBack = { viewModel.onAction(DnsRecordIntent.GoBack) },
                modifier = modifier
            )
        }
    }

    if (selected != null) {
        val recordId = selected.record.id
        DnsRecordDetailDrawer(
            item = selected,
            noteDraft = state.noteDraft,
            working = state.working,
            message = state.message,
            onDismiss = { viewModel.onAction(DnsRecordIntent.DismissDetailedDrawer) },
            onNoteChange = { viewModel.onAction(DnsRecordIntent.UpdateNoteDraft(it)) },
            onSaveNote = { viewModel.onAction(DnsRecordIntent.SaveNote) },
            onToggleLock = { viewModel.onAction(DnsRecordIntent.ToggleLock(recordId)) },
            onEdit = { viewModel.onAction(DnsRecordIntent.EditRecord(recordId)) },
            onDelete = { viewModel.onAction(DnsRecordIntent.RequestDelete(recordId)) },
        )
    }
}
