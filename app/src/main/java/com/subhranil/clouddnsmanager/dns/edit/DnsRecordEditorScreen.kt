package com.subhranil.clouddnsmanager.dns.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.dns.nav.DnsDestination
import com.subhranil.clouddnsmanager.dns.record.DNS_COMMENT_MAX_LENGTH
import com.subhranil.clouddnsmanager.dns.record.EDITABLE_DNS_TYPES
import com.subhranil.clouddnsmanager.dns.record.supportsProxy
import com.subhranil.clouddnsmanager.dns.record.ttlLabel
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Full-screen form to add a record or edit one. Saving goes through the ViewModel and AuthGate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsRecordEditorScreen(
    destination: DnsDestination.Editor,
    modifier: Modifier = Modifier,
    viewModel: DnsRecordEditorViewModel = koinViewModel { parametersOf(destination) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction

    BackHandler { onAction(DnsRecordEditorIntent.Back) }

    DnsRecordEditorContent(state = state, onAction = onAction, modifier = modifier)
}

/** Stateless editor (also used by previews). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsRecordEditorContent(
    state: DnsRecordEditorState,
    onAction: (DnsRecordEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isNew) "Add record" else "Edit record", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(DnsRecordEditorIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                // No top-bar Save: the single "Create record" / "Save changes" button at the
                // bottom of the form is the only save action (it also shows the saving state).
            )
        },
    ) { innerPadding ->
        when (val load = state.loadState) {
            DnsEditorLoadState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is DnsEditorLoadState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Error icon",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(load.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = { onAction(DnsRecordEditorIntent.RetryLoad) }) { Text("Retry") }
            }

            DnsEditorLoadState.Ready -> EditorForm(
                state = state,
                onAction = onAction,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun EditorForm(
    state: DnsRecordEditorState,
    onAction: (DnsRecordEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val enabled = state.readOnlyReason == null && !state.saving
    val field = @Composable { f: DnsField, label: String, value: String, hint: String?, keyboard: KeyboardType ->
        FormField(
            label = label,
            value = value,
            error = state.errors[f],
            hint = hint,
            enabled = enabled,
            keyboardType = keyboard,
            onValueChange = { onAction(DnsRecordEditorIntent.UpdateField(f, it)) },
        )
    }
    val zone = state.zoneName ?: "your zone"

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.readOnlyReason?.let { reason ->
            Notice(icon = { Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary) }, text = reason)
        }
        state.saveError?.let { error ->
            Notice(
                icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
                text = error,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // --- Type ---
        SectionLabel("Type")
        if (state.isNew) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                EDITABLE_DNS_TYPES.forEach { type ->
                    FilterChip(
                        selected = form.type == type,
                        onClick = { onAction(DnsRecordEditorIntent.SetType(type)) },
                        label = { Text(type.name) },
                        enabled = enabled,
                    )
                }
            }
        } else {
            Text(form.type.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // --- Name ---
        when (form.type) {
            DnsRecordType.SRV -> {
                field(DnsField.SRV_SERVICE, "Service", form.srvService, "e.g. _sip", KeyboardType.Text)
                SectionLabel("Protocol")
                ChipChoice(
                    options = SRV_PROTOCOLS,
                    selected = form.srvProto,
                    enabled = enabled,
                    error = state.errors[DnsField.SRV_PROTO],
                    onSelect = { onAction(DnsRecordEditorIntent.UpdateField(DnsField.SRV_PROTO, it)) },
                )
                field(DnsField.NAME, "Name (optional)", form.name, "Leave empty or @ for $zone", KeyboardType.Uri)
            }
            else -> field(DnsField.NAME, "Name", form.name, "Use @ for $zone, or e.g. www", KeyboardType.Uri)
        }

        // --- Content per type ---
        when (form.type) {
            DnsRecordType.A -> field(DnsField.CONTENT, "IPv4 address", form.content, "e.g. 192.0.2.1", KeyboardType.Uri)
            DnsRecordType.AAAA -> field(DnsField.CONTENT, "IPv6 address", form.content, "e.g. 2001:db8::1", KeyboardType.Uri)
            DnsRecordType.CNAME -> field(DnsField.CONTENT, "Target", form.content, "e.g. example.net", KeyboardType.Uri)
            DnsRecordType.NS -> field(DnsField.CONTENT, "Nameserver", form.content, "e.g. ns1.example.net", KeyboardType.Uri)
            DnsRecordType.MX -> {
                field(DnsField.CONTENT, "Mail server", form.content, "e.g. mx.example.net", KeyboardType.Uri)
                field(DnsField.PRIORITY, "Priority", form.priority, "0-65535, lower is preferred", KeyboardType.Number)
            }
            DnsRecordType.TXT -> FormField(
                label = "Content",
                value = form.content,
                error = state.errors[DnsField.CONTENT],
                hint = "e.g. v=spf1 include:_spf.example.net ~all",
                enabled = enabled,
                singleLine = false,
                onValueChange = { onAction(DnsRecordEditorIntent.UpdateField(DnsField.CONTENT, it)) },
            )
            DnsRecordType.CAA -> {
                field(DnsField.CAA_FLAGS, "Flags", form.caaFlags, "0-255, usually 0", KeyboardType.Number)
                SectionLabel("Tag")
                ChipChoice(
                    options = CAA_TAGS,
                    selected = form.caaTag,
                    enabled = enabled,
                    error = state.errors[DnsField.CAA_TAG],
                    onSelect = { onAction(DnsRecordEditorIntent.UpdateField(DnsField.CAA_TAG, it)) },
                )
                field(DnsField.CAA_VALUE, "Value", form.caaValue, "e.g. letsencrypt.org, or mailto: for iodef", KeyboardType.Uri)
            }
            DnsRecordType.SRV -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { field(DnsField.SRV_PRIORITY, "Priority", form.srvPriority, null, KeyboardType.Number) }
                    Box(Modifier.weight(1f)) { field(DnsField.SRV_WEIGHT, "Weight", form.srvWeight, null, KeyboardType.Number) }
                    Box(Modifier.weight(1f)) { field(DnsField.SRV_PORT, "Port", form.srvPort, null, KeyboardType.Number) }
                }
                field(DnsField.SRV_TARGET, "Target", form.srvTarget, "e.g. sip.example.com", KeyboardType.Uri)
            }
            else -> Unit
        }

        // --- Proxy ---
        if (form.type.supportsProxy()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Proxy through Cloudflare", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (form.proxied) "Traffic goes through Cloudflare (orange cloud)" else "DNS only (grey cloud)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = form.proxied,
                    onCheckedChange = { onAction(DnsRecordEditorIntent.SetProxied(it)) },
                    enabled = enabled,
                )
            }
        }

        // --- TTL ---
        TtlPicker(
            ttl = form.ttl,
            options = state.ttlOptions,
            proxied = form.proxied && form.type.supportsProxy(),
            enabled = enabled,
            error = state.errors[DnsField.TTL],
            onSelect = { onAction(DnsRecordEditorIntent.SetTtl(it)) },
        )

        // --- Cloudflare comment ---
        FormField(
            label = "Comment",
            value = form.comment,
            error = state.errors[DnsField.COMMENT],
            hint = "${form.comment.length}/$DNS_COMMENT_MAX_LENGTH · Saved on Cloudflare and shown in the dashboard",
            enabled = enabled,
            singleLine = false,
            onValueChange = { onAction(DnsRecordEditorIntent.UpdateField(DnsField.COMMENT, it)) },
        )

        if (state.readOnlyReason == null) {
            Button(
                onClick = { onAction(DnsRecordEditorIntent.Save) },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Saving…")
                } else {
                    Text(if (state.isNew) "Create record" else "Save changes")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    error: String?,
    hint: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: hint)?.let { text -> { Text(text) } },
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ChipChoice(
    options: List<String>,
    selected: String,
    enabled: Boolean,
    error: String?,
    onSelect: (String) -> Unit,
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    enabled = enabled,
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Auto or one of the common TTLs. Proxied records are always Auto on Cloudflare. */
@Composable
private fun TtlPicker(
    ttl: Int,
    options: List<Int>,
    proxied: Boolean,
    enabled: Boolean,
    error: String?,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        SectionLabel("TTL")
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled && !proxied,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (proxied) "Auto" else ttlLabel(ttl), modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(ttlLabel(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
        val help = error ?: if (proxied) "Proxied records always use Auto TTL." else null
        help?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Notice(icon: @Composable () -> Unit, text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}
