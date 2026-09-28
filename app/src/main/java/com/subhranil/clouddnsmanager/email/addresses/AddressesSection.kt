package com.subhranil.clouddnsmanager.email.addresses

import com.subhranil.clouddnsmanager.email.components.CopyButton
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.email.EmailZone
import com.subhranil.clouddnsmanager.email.components.ConfirmDestructiveDialog
import com.subhranil.clouddnsmanager.email.components.EmailEmpty
import com.subhranil.clouddnsmanager.email.components.EmailError
import com.subhranil.clouddnsmanager.email.components.EmailLoading
import com.subhranil.clouddnsmanager.email.components.LockCard
import com.subhranil.clouddnsmanager.email.components.MessageEffect
import com.subhranil.clouddnsmanager.email.components.NoteCard
import com.subhranil.clouddnsmanager.email.components.formatTimestamp
import com.subhranil.clouddnsmanager.email.model.EmailDestinationAddress
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val VerifiedGreen = Color(0xFF137333)
private val PendingAmber = Color(0xFFB06000)

/** The Addresses tab (also the body of the stand-alone Addresses screen). */
@Composable
fun AddressesSection(
    zone: EmailZone,
    modifier: Modifier = Modifier,
    viewModel: AddressListViewModel = koinViewModel { parametersOf(zone) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    val snackbarHostState = remember { SnackbarHostState() }
    MessageEffect(state.message, snackbarHostState) { onAction(AddressListIntent.MessageShown) }

    state.addForm?.let { AddAddressDialog(it, onAction) }

    state.verificationSentTo?.let { email ->
        AlertDialog(
            onDismissRequest = { onAction(AddressListIntent.DismissVerificationInfo) },
            icon = { Icon(Icons.Filled.Email, contentDescription = null) },
            title = { Text("Check that inbox") },
            text = {
                Text(
                    "Cloudflare sent a verification email to $email. The address can't receive forwarded mail " +
                        "until someone clicks the link in that email. It shows as \"Pending verification\" until then."
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(AddressListIntent.DismissVerificationInfo) }) { Text("OK") }
            },
        )
    }

    state.address(state.confirmDeleteId)?.let { address ->
        val usedBy = state.aliasesUsing(address.email)
        val warning = when {
            usedBy == null -> "\n\nCouldn't check whether any alias in ${state.zoneName} still forwards here."
            usedBy.isEmpty() -> ""
            else -> "\n\nWarning: ${usedBy.size} alias(es) in ${state.zoneName} forward here and will stop delivering: " +
                usedBy.take(5).joinToString(", ") + if (usedBy.size > 5) ", …" else "."
        }
        ConfirmDestructiveDialog(
            title = "Delete ${address.email}?",
            text = "It will be removed from your Cloudflare account and can't receive forwarded mail any more.$warning",
            confirmLabel = "Delete",
            onConfirm = { onAction(AddressListIntent.ConfirmDelete) },
            onDismiss = { onAction(AddressListIntent.DismissDelete) },
        )
    }

    state.address(state.selectedId)?.let { address ->
        AddressDetailSheet(
            address = address,
            locked = address.addressId in state.lockedAddressIds,
            note = state.notes[address.addressId],
            usedBy = state.aliasesUsing(address.email),
            deleting = state.deletingId == address.addressId,
            onAction = onAction,
        )
    }

    AddressListContent(state = state, onAction = onAction, modifier = modifier, snackbarHostState = snackbarHostState)
}

/** Stateless Addresses tab (also used by previews). Dialogs and the detail sheet stay in [AddressesSection]. */
@Composable
fun AddressListContent(
    state: AddressListState,
    onAction: (AddressListIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (val data = state.dataState) {
            AddressListDataState.Loading -> EmailLoading()
            is AddressListDataState.Error -> EmailError(data.message, onRetry = { onAction(AddressListIntent.Retry) })
            is AddressListDataState.Loaded -> if (data.addresses.isEmpty()) {
                EmailEmpty(
                    title = "No destination addresses",
                    body = "Add the inboxes your aliases should forward to. Each one must be verified by clicking a link Cloudflare emails to it.",
                    actionLabel = "Add address",
                    onAction = { onAction(AddressListIntent.ShowAdd) },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                ) {
                    item {
                        Text(
                            "Destination addresses belong to your Cloudflare account and are shared by all its zones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(data.addresses, key = { it.addressId }) { address ->
                        AddressRow(
                            address = address,
                            locked = address.addressId in state.lockedAddressIds,
                            hasNote = state.notes.containsKey(address.addressId),
                            onClick = { onAction(AddressListIntent.Select(address.addressId)) },
                        )
                    }
                }
            }
        }
        if (state.dataState is AddressListDataState.Loaded) {
            ExtendedFloatingActionButton(
                onClick = { onAction(AddressListIntent.ShowAdd) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add address") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
}

@Composable
private fun VerificationBadge(address: EmailDestinationAddress) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (address.isVerified) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (address.isVerified) VerifiedGreen else PendingAmber,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (address.isVerified) "Verified" else "Pending verification",
            style = MaterialTheme.typography.labelMedium,
            color = if (address.isVerified) VerifiedGreen else PendingAmber,
        )
    }
}

@Composable
private fun AddressRow(address: EmailDestinationAddress, locked: Boolean, hasNote: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    address.email,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                VerificationBadge(address)
            }
            if (locked) {
                Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
            }
            if (hasNote) {
                Icon(Icons.Filled.Create, contentDescription = "Has a note", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun AddAddressDialog(form: AddAddressForm, onAction: (AddressListIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(AddressListIntent.DismissAdd) },
        title = { Text("Add destination address") },
        text = {
            Column {
                Text(
                    "Cloudflare will email a verification link to this address. Aliases can forward to it once the link is clicked.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = form.email,
                    onValueChange = { onAction(AddressListIntent.UpdateAddEmail(it)) },
                    label = { Text("Email address") },
                    singleLine = true,
                    enabled = !form.saving,
                    isError = form.error != null,
                    supportingText = form.error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAction(AddressListIntent.SubmitAdd) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            if (form.saving) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = { onAction(AddressListIntent.SubmitAdd) }) { Text("Add") }
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(AddressListIntent.DismissAdd) }, enabled = !form.saving) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressDetailSheet(
    address: EmailDestinationAddress,
    locked: Boolean,
    note: String?,
    usedBy: List<String>?,
    deleting: Boolean,
    onAction: (AddressListIntent) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(AddressListIntent.DismissSelected) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectionContainer(Modifier.weight(1f)) {
                    Text(address.email, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                CopyButton(text = address.email, label = "Email address")
            }
            VerificationBadge(address)
            if (!address.isVerified) {
                Text(
                    "Cloudflare sent a verification email to this address. It can't be used by aliases until the link in it is clicked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            address.verified?.let { Text("Verified: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall) }
            address.created?.let { Text("Added: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall) }
            Text(
                when {
                    usedBy == null -> "Couldn't check which aliases use this address."
                    usedBy.isEmpty() -> "No alias in this zone forwards here."
                    else -> "Used by: " + usedBy.joinToString(", ")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LockCard(
                locked = locked,
                itemLabel = "address",
                onLockedChange = { onAction(AddressListIntent.SetLocked(address.addressId, it)) },
            )
            NoteCard(savedNote = note, onSave = { onAction(AddressListIntent.SaveNote(address.addressId, it)) })

            OutlinedButton(
                onClick = { onAction(AddressListIntent.RequestDelete(address.addressId)) },
                enabled = !locked && !deleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (deleting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(if (locked) "Unlock to delete" else "Delete address", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
