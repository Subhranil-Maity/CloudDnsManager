package com.subhranil.clouddnsmanager.email.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.email.components.formatTimestamp
import com.subhranil.clouddnsmanager.email.model.EmailActivityEvent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** The zone-wide Activity tab of the Email home. */
@Composable
fun ActivitySection(
    zoneId: String,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinViewModel(key = "email-activity-zone") { parametersOf(zoneId, "") },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ActivityDetailSheetHost(state, viewModel::onAction)
    ActivityContent(state = state, onAction = viewModel::onAction, modifier = modifier)
}

/** Stateless zone-wide Activity tab (also used by previews). */
@Composable
fun ActivityContent(
    state: ActivityState,
    onAction: (ActivityIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            activityItems(state, onAction)
        }
    }
}

/** Shows the details sheet for the selected event, if any. */
@Composable
fun ActivityDetailSheetHost(state: ActivityState, onAction: (ActivityIntent) -> Unit) {
    state.selected?.let { ActivityDetailSheet(it, onDismiss = { onAction(ActivityIntent.DismissSelected) }) }
}

/**
 * The activity list as LazyColumn items, so it can sit inside another screen's list
 * (the alias details) as well as fill the Activity tab.
 */
fun LazyListScope.activityItems(state: ActivityState, onAction: (ActivityIntent) -> Unit) {
    item(key = "activity-retention") {
        Text(
            "Showing up to the last ${if (state.windowDays == 1) "24 hours" else "${state.windowDays} days"}. " +
                "Cloudflare only keeps email activity for a short time (it depends on your plan), " +
                "so older messages don't appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    when (val data = state.dataState) {
        ActivityDataState.Loading -> item(key = "activity-loading") {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ActivityDataState.Error -> item(key = "activity-error") {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(data.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { onAction(ActivityIntent.Retry) }) { Text("Retry") }
            }
        }
        is ActivityDataState.Loaded -> if (data.events.isEmpty()) {
            item(key = "activity-empty") {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (state.alias != null) "No mail to ${state.alias} in this period." else "No mail received in this period.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { onAction(ActivityIntent.Retry) }) { Text("Refresh") }
                }
            }
        } else {
            itemsIndexed(data.events, key = { index, event -> "activity-${event.id ?: index}-$index" }) { _, event ->
                ActivityRow(event, showRecipient = state.alias == null, onClick = { onAction(ActivityIntent.Select(event)) })
            }
        }
    }
}

private fun statusColors(status: String?): Pair<Color, Color> = when (status?.lowercase()) {
    "delivered" -> Color(0xFFE6F4EA) to Color(0xFF137333)
    "dropped", "rejected" -> Color(0xFFFCE8E6) to Color(0xFFC5221F)
    null, "" -> Color(0xFFF1F3F4) to Color(0xFF5F6368)
    else -> Color(0xFFFEF7E0) to Color(0xFFB06000) // failed / deferred / unknown
}

/** "deliveryFailed" → "Delivery failed". */
private fun humanize(value: String?): String {
    if (value.isNullOrBlank()) return "Unknown"
    val spaced = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ').lowercase()
    return spaced.replaceFirstChar { it.uppercase() }
}

@Composable
private fun ActivityRow(event: EmailActivityEvent, showRecipient: Boolean, onClick: () -> Unit) {
    val (bg, fg) = statusColors(event.status)
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    event.from?.takeIf { it.isNotBlank() } ?: "(unknown sender)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    event.subject?.takeIf { it.isNotBlank() } ?: "(no subject)",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showRecipient && !event.to.isNullOrBlank()) {
                    Text(
                        "to ${event.to}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    formatTimestamp(event.datetime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
                Text(
                    humanize(event.status),
                    color = fg,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDetailSheet(event: EmailActivityEvent, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Message details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            DetailLine("Received", formatTimestamp(event.datetime))
            DetailLine("From", event.from)
            DetailLine("To", event.to)
            DetailLine("Subject", event.subject)
            DetailLine("Status", humanize(event.status))
            DetailLine("Action", event.action?.let { humanize(it) })
            DetailLine("SPF", event.spf)
            DetailLine("DKIM", event.dkim)
            DetailLine("DMARC", event.dmarc)
            DetailLine("ARC", event.arc)
            if (event.isSpam != 0) DetailLine("Spam", "Flagged as spam" + (event.spamScore?.let { " (score $it)" } ?: ""))
            if (event.isNDR != 0) DetailLine("Bounce", "This was a non-delivery report")
            DetailLine("Error", event.errorDetail)
            DetailLine("Message-ID", event.messageId)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
