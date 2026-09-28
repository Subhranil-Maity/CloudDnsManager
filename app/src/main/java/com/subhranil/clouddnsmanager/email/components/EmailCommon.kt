package com.subhranil.clouddnsmanager.email.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Centered spinner for a section that is loading. */
@Composable
fun EmailLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Error message with a Retry button, same look as the zone list's error screen. */
@Composable
fun EmailError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

/** Empty list placeholder with an optional call to action. */
@Composable
fun EmailEmpty(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Confirmation before a destructive action; the confirm button is tinted with the error color. */
@Composable
fun ConfirmDestructiveDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Shows [message] once as a snackbar, then reports it as shown so the ViewModel can clear it. */
@Composable
fun MessageEffect(message: String?, snackbarHostState: SnackbarHostState, onShown: () -> Unit) {
    // Shown from a scope that outlives this effect: clearing the message re-keys the effect,
    // which must not cancel the snackbar that is still on screen.
    val scope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (message != null) {
            scope.launch { snackbarHostState.showSnackbar(message) }
            onShown()
        }
    }
}

/**
 * The local lock switch. Locking needs no authentication; unlocking asks for it
 * (the ViewModel goes through ItemLockManager, which runs the AuthGate).
 */
@Composable
fun LockCard(locked: Boolean, itemLabel: String, onLockedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = if (locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (locked) "Locked" else "Not locked", fontWeight = FontWeight.SemiBold)
                Text(
                    if (locked) "This $itemLabel can't be edited or deleted until you unlock it. Unlocking needs your PIN or biometrics."
                    else "Lock this $itemLabel to protect it from accidental changes. Stored only on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = locked, onCheckedChange = onLockedChange)
        }
    }
}

/**
 * A private note on an item. The draft lives in the composable; saving goes through the
 * ViewModel. Notes are local only and never sent to Cloudflare.
 */
@Composable
fun NoteCard(savedNote: String?, onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    var draft by rememberSaveable(savedNote) { mutableStateOf(savedNote.orEmpty()) }
    val changed = draft.trim() != savedNote.orEmpty()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Private note", fontWeight = FontWeight.SemiBold)
            Text(
                "Stored only on this device. Never sent to Cloudflare.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Used for the newsletter at shop.example") },
                minLines = 2,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!savedNote.isNullOrEmpty()) {
                    TextButton(onClick = { draft = ""; onSave("") }) { Text("Clear") }
                }
                TextButton(onClick = { onSave(draft) }, enabled = changed) { Text("Save note") }
            }
        }
    }
}

/** "Jan 19, 2026, 10:51 AM" in the device's time zone; the raw text if it isn't an ISO instant. */
fun formatTimestamp(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        iso
    }
}
