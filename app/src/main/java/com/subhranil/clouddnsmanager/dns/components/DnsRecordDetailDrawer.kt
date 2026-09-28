package com.subhranil.clouddnsmanager.dns.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subhranil.clouddnsmanager.dns.DnsRecordItem
import com.subhranil.clouddnsmanager.dns.record.ttlLabel
import com.subhranil.clouddnsmanager.localstore.lock.LockStatus

/**
 * Record details plus the actions for one record. Every action is an intent handled by the
 * ViewModel (which runs the confirmation / AuthGate / API steps); nothing here writes to
 * Cloudflare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsRecordDetailDrawer(
    item: DnsRecordItem,
    noteDraft: String,
    working: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onToggleLock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val record = item.record
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val copyToClipboard = { textToCopy: String, label: String ->
        clipboardManager.setText(AnnotatedString(textToCopy))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .imePadding()
        ) {
            // --- Header ---
            Text(
                text = "Record Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LockBanner(item.lockStatus)
            if (!item.typeEditable) {
                Banner(
                    icon = Icons.Filled.Info,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Read-only in this app",
                    body = "${record.type.name} records can't be edited here yet. Use the Cloudflare dashboard to change them.",
                )
            }

            // --- Grid Metadata Rows ---
            DetailRowItem(label = "Type", value = record.type.name, isMonospace = true)

            DetailRowItem(
                label = "Name / Host",
                value = record.name,
                isMonospace = true,
                onCopy = { copyToClipboard(record.name, "Name") }
            )

            DetailRowItem(
                label = "Content / Value",
                value = record.content,
                isMonospace = true,
                onCopy = { copyToClipboard(record.content, "Value") }
            )

            DetailRowItem(
                label = "TTL",
                value = if (record.ttl == 1) "Auto (Default)" else "${ttlLabel(record.ttl)} (${record.ttl} seconds)"
            )

            if (record.proxiable) {
                DetailRowItem(
                    label = "Routing Status",
                    value = if (record.proxied) "Proxied through Cloudflare" else "Bypassed (DNS Only)"
                )
            }

            if (record.priority != null) {
                DetailRowItem(label = "Priority", value = record.priority.toString())
            }

            DetailRowItem(
                label = "Cloudflare comment (visible in the dashboard)",
                value = item.comment ?: "None"
            )

            // --- Private note: local only, clearly separate from Cloudflare's comment ---
            Spacer(modifier = Modifier.height(16.dp))
            Text("Private note", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "Stored only on this device. It's never sent to Cloudflare.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteDraft,
                onValueChange = onNoteChange,
                placeholder = { Text("e.g. Points at the office VPN, ask Sam before changing") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            val noteChanged = noteDraft.trim() != item.note.orEmpty()
            TextButton(
                onClick = onSaveNote,
                enabled = noteChanged,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (noteDraft.isBlank() && item.hasNote) "Remove note" else "Save note")
            }

            // --- Actions ---
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            val editable = item.lockStatus == LockStatus.Unlocked
            if (working) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onToggleLock,
                    enabled = !working && item.lockStatus !is LockStatus.Managed,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (item.lockStatus == LockStatus.UserLocked) "Unlock" else "Lock")
                }
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !working && editable && item.typeEditable,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !working && editable,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}

/** Explains why the actions are disabled, if they are. */
@Composable
private fun LockBanner(status: LockStatus) {
    when (status) {
        is LockStatus.Managed -> Banner(
            icon = Icons.Filled.Lock,
            tint = CloudflareOrange,
            title = "Locked by Cloudflare",
            body = "${status.reason}. Cloudflare doesn't allow editing, deleting or locking it here.",
        )
        LockStatus.UserLocked -> Banner(
            icon = Icons.Filled.Lock,
            tint = MaterialTheme.colorScheme.primary,
            title = "Locked",
            body = "Unlock it to edit or delete. Unlocking asks for your PIN or biometrics. " +
                "The lock is saved in the record's Cloudflare comment, so it also shows in the dashboard.",
        )
        LockStatus.Unlocked -> Unit
    }
}

@Composable
private fun Banner(icon: ImageVector, tint: Color, title: String, body: String) {
    Surface(
        color = tint.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tint)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
