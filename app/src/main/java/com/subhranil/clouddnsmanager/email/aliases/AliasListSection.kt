package com.subhranil.clouddnsmanager.email.aliases

import com.subhranil.clouddnsmanager.email.components.CopyButton
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.email.EmailZone
import com.subhranil.clouddnsmanager.email.components.EmailEmpty
import com.subhranil.clouddnsmanager.email.components.EmailError
import com.subhranil.clouddnsmanager.email.components.EmailLoading
import com.subhranil.clouddnsmanager.email.components.MessageEffect
import com.subhranil.clouddnsmanager.email.domain.AliasDisplay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** The Aliases tab of the Email home. */
@Composable
fun AliasListSection(
    zone: EmailZone,
    modifier: Modifier = Modifier,
    viewModel: AliasListViewModel = koinViewModel { parametersOf(zone) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    MessageEffect(state.message, snackbarHostState) { viewModel.onAction(AliasListIntent.MessageShown) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val data = state.dataState) {
            AliasListDataState.Loading -> EmailLoading()
            is AliasListDataState.Error -> EmailError(data.message, onRetry = { viewModel.onAction(AliasListIntent.Retry) })
            is AliasListDataState.Loaded -> if (data.aliases.isEmpty() && data.catchAll == null) {
                EmailEmpty(
                    title = "No aliases yet",
                    body = "An alias is an address at ${state.zoneName} that forwards mail to one of your inboxes.",
                    actionLabel = "Create alias",
                    onAction = { viewModel.onAction(AliasListIntent.Create) },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                ) {
                    item {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = { viewModel.onAction(AliasListIntent.Search(it)) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            placeholder = { Text("Search aliases") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onAction(AliasListIntent.Search("")) }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                        )
                    }
                    data.catchAll?.let { catchAll ->
                        if (state.query.isBlank()) item { CatchAllCard(catchAll) }
                    }
                    val visible = state.visibleAliases
                    if (visible.isEmpty()) {
                        item {
                            Text(
                                if (data.aliases.isEmpty()) "No aliases yet. Tap \"New alias\" to create one."
                                else "No aliases match \"${state.query}\".",
                                modifier = Modifier.padding(vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(visible, key = { it.rule.ruleId }) { row ->
                        val ruleId = row.rule.ruleId
                        AliasRowItem(
                            alias = row.display,
                            locked = ruleId in state.lockedRuleIds,
                            hasNote = ruleId in state.notedRuleIds,
                            busy = ruleId in state.busyRuleIds,
                            onClick = { viewModel.onAction(AliasListIntent.Open(ruleId)) },
                            onEnabledChange = { viewModel.onAction(AliasListIntent.SetEnabled(ruleId, it)) },
                        )
                    }
                }
            }
        }

        if (state.dataState is AliasListDataState.Loaded) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onAction(AliasListIntent.Create) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New alias") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
}

@Composable
private fun AliasRowItem(
    alias: AliasDisplay,
    locked: Boolean,
    hasNote: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        alias.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        color = if (alias.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (locked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    if (hasNote) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Create, contentDescription = "Has a note", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    alias.targetSummary,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                alias.name?.takeIf { it.isNotBlank() && alias.address != null }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            alias.address?.let { address ->
                CopyButton(text = address, label = "Email address")
            }
            Spacer(Modifier.width(4.dp))
            if (busy) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Switch(checked = alias.enabled, onCheckedChange = onEnabledChange, enabled = !locked)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

/** The catch-all rule is shown read-only: it's managed in the Cloudflare dashboard. */
@Composable
private fun CatchAllCard(catchAll: AliasDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Catch-all: ${if (catchAll.enabled) "on" else "off"}", fontWeight = FontWeight.SemiBold)
            Text(
                if (catchAll.enabled) "Mail to any other address: ${catchAll.targetSummary}"
                else "Mail to unknown addresses is rejected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Change it in the Cloudflare dashboard.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
