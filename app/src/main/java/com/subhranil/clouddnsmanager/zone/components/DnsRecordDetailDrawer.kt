package com.subhranil.clouddnsmanager.zone.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subhranil.clouddnsmanager.models.dns.DnsRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsRecordDetailDrawer(
    record: DnsRecord,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // --- Header ---
            Text(
                text = "Record Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 20.dp)
            )

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
                value = if (record.ttl == 1) "Auto (Default)" else "${record.ttl} seconds"
            )

            DetailRowItem(
                label = "Routing Status",
                value = if (record.proxied) "Proxied through Cloudflare" else "Bypassed (DNS Only)"
            )

            if (record.priority != null) {
                DetailRowItem(label = "Priority", value = record.priority.toString())
            }
        }
    }
}


