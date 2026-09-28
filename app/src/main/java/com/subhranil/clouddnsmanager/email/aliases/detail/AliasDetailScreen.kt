package com.subhranil.clouddnsmanager.email.aliases.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.email.activity.ActivityDetailSheetHost
import com.subhranil.clouddnsmanager.email.activity.ActivityIntent
import com.subhranil.clouddnsmanager.email.activity.ActivityState
import com.subhranil.clouddnsmanager.email.activity.ActivityViewModel
import com.subhranil.clouddnsmanager.email.activity.activityItems
import com.subhranil.clouddnsmanager.email.components.ConfirmDestructiveDialog
import com.subhranil.clouddnsmanager.email.components.CopyButton
import com.subhranil.clouddnsmanager.email.components.EmailError
import com.subhranil.clouddnsmanager.email.components.EmailLoading
import com.subhranil.clouddnsmanager.email.components.LockCard
import com.subhranil.clouddnsmanager.email.components.MessageEffect
import com.subhranil.clouddnsmanager.email.components.NoteCard
import com.subhranil.clouddnsmanager.email.domain.AliasDisplay
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliasDetailScreen(
    destination: EmailDestination.AliasDetail,
    modifier: Modifier = Modifier,
    viewModel: AliasDetailViewModel = koinViewModel { parametersOf(destination) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    val snackbarHostState = remember { SnackbarHostState() }
    MessageEffect(state.message, snackbarHostState) { onAction(AliasDetailIntent.MessageShown) }

    BackHandler { onAction(AliasDetailIntent.Back) }

    val loaded = state.dataState as? AliasDetailDataState.Loaded
    if (state.confirmDelete && loaded != null) {
        ConfirmDestructiveDialog(
            title = "Delete ${loaded.display.title}?",
            text = "Mail sent to this address will no longer be forwarded. This can't be undone. " +
                "Its private note and lock on this device are removed too.",
            confirmLabel = "Delete",
            onConfirm = { onAction(AliasDetailIntent.ConfirmDelete) },
            onDismiss = { onAction(AliasDetailIntent.DismissDelete) },
        )
    }

    // Per-alias activity: its own ViewModel, filtered by the alias address (only for aliases
    // with a concrete address; the catch-all has none)
    val address = loaded?.display?.address
    val activityViewModel: ActivityViewModel? = address?.let {
        koinViewModel(key = "email-activity-$it") { parametersOf(destination.zoneId, it) }
    }
    val activityState = activityViewModel?.state?.collectAsStateWithLifecycle()?.value
    if (activityViewModel != null && activityState != null) {
        ActivityDetailSheetHost(activityState, activityViewModel::onAction)
    }

    AliasDetailScreenContent(
        state = state,
        onAction = onAction,
        activityState = activityState,
        onActivityAction = { intent -> activityViewModel?.onAction(intent) },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** Stateless alias details screen (also used by previews). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliasDetailScreenContent(
    state: AliasDetailState,
    onAction: (AliasDetailIntent) -> Unit,
    activityState: ActivityState?,
    onActivityAction: (ActivityIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alias", fontWeight = FontWeight.Bold)
                        Text(
                            state.zoneName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(AliasDetailIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (val data = state.dataState) {
                AliasDetailDataState.Loading -> EmailLoading()
                is AliasDetailDataState.Error -> EmailError(data.message, onRetry = { onAction(AliasDetailIntent.Retry) })
                is AliasDetailDataState.Loaded ->
                    AliasDetailContent(state, data.display, onAction, activityState, onActivityAction)
            }
        }
    }
}

@Composable
private fun AliasDetailContent(
    state: AliasDetailState,
    display: AliasDisplay,
    onAction: (AliasDetailIntent) -> Unit,
    activityState: ActivityState?,
    onActivityAction: (ActivityIntent) -> Unit,
) {
    val canChange = !state.locked && !state.busy
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "summary") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Selectable too, so a long-press works as well as the copy button
                        SelectionContainer(Modifier.weight(1f)) {
                            Text(
                                display.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            )
                        }
                        display.address?.let { address ->
                            CopyButton(text = address, label = "Email address")
                        }
                    }
                    Text(display.targetSummary, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                    display.name?.takeIf { it.isNotBlank() && display.address != null }?.let {
                        Text("Rule name: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (display.enabled) "Enabled" else "Disabled", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (display.enabled) "Mail to this address is delivered." else "Mail to this address is rejected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.togglingEnabled) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Switch(
                                checked = display.enabled,
                                onCheckedChange = { onAction(AliasDetailIntent.SetEnabled(it)) },
                                enabled = canChange,
                            )
                        }
                    }
                }
            }
        }

        item(key = "edit") {
            val form = state.edit
            if (form == null) {
                OutlinedButton(
                    onClick = { onAction(AliasDetailIntent.StartEdit) },
                    enabled = canChange,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.locked) "Unlock to edit" else "Edit destination or name")
                }
            } else {
                EditCard(form, state.verifiedDestinations, onAction)
            }
        }

        item(key = "lock") {
            LockCard(
                locked = state.locked,
                itemLabel = "alias",
                onLockedChange = { onAction(AliasDetailIntent.SetLocked(it)) },
            )
        }
        item(key = "note") {
            NoteCard(savedNote = state.note, onSave = { onAction(AliasDetailIntent.SaveNote(it)) })
        }
        item(key = "delete") {
            OutlinedButton(
                onClick = { onAction(AliasDetailIntent.RequestDelete) },
                enabled = canChange,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.deleting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.locked) "Unlock to delete" else "Delete alias", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (activityState != null) {
            item(key = "activity-header") {
                Text("Recent mail to this alias", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            }
            activityItems(activityState, onActivityAction)
        }
    }
}

@Composable
private fun EditCard(form: AliasEditForm, verified: List<String>?, onAction: (AliasDetailIntent) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Edit alias", style = MaterialTheme.typography.titleSmall)
            if (form.destination != null) {
                Text("Forward to", style = MaterialTheme.typography.labelLarge)
                // The current destination stays selectable even if it's no longer in the verified list
                val options = (listOfNotNull(form.destination.takeIf { it.isNotBlank() }) + verified.orEmpty())
                    .distinctBy { it.lowercase() }
                if (verified == null) {
                    Text(
                        "Couldn't load your verified addresses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.selectableGroup()) {
                    options.forEach { email ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = form.destination == email,
                                    onClick = { onAction(AliasDetailIntent.SelectEditDestination(email)) },
                                    enabled = !form.saving,
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = form.destination == email, onClick = null, enabled = !form.saving)
                            Spacer(Modifier.width(8.dp))
                            Text(email, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            OutlinedTextField(
                value = form.name,
                onValueChange = { onAction(AliasDetailIntent.UpdateEditName(it)) },
                label = { Text("Rule name (optional)") },
                singleLine = true,
                enabled = !form.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onAction(AliasDetailIntent.CancelEdit) }, enabled = !form.saving) { Text("Cancel") }
                if (form.saving) {
                    CircularProgressIndicator(Modifier.size(24.dp).padding(start = 8.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { onAction(AliasDetailIntent.SaveEdit) }) { Text("Save") }
                }
            }
        }
    }
}
