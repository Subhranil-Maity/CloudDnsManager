package com.subhranil.clouddnsmanager.email.aliases.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAliasScreen(
    destination: EmailDestination.CreateAlias,
    modifier: Modifier = Modifier,
    viewModel: CreateAliasViewModel = koinViewModel { parametersOf(destination) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction

    BackHandler { onAction(CreateAliasIntent.Back) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("New alias", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onAction(CreateAliasIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Address ──
            Text("Alias address", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = state.localPart,
                onValueChange = { onAction(CreateAliasIntent.UpdateLocalPart(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name before the @") },
                suffix = { Text("@${state.zoneName}") },
                singleLine = true,
                enabled = !state.saving,
                isError = state.localPartError != null,
                supportingText = state.localPartError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
            )
            FilledTonalButton(onClick = { onAction(CreateAliasIntent.GenerateRandom) }, enabled = !state.saving) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate random")
            }
            Text(
                state.fullAddress,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )

            // ── Destination ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Forward to", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { onAction(CreateAliasIntent.RetryDestinations) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh addresses")
                }
            }
            DestinationPicker(state, onAction)

            // ── Optional name ──
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(CreateAliasIntent.UpdateName(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Rule name (optional)") },
                placeholder = { Text("e.g. Newsletter sign-ups") },
                singleLine = true,
                enabled = !state.saving,
            )

            state.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = { onAction(CreateAliasIntent.Save) },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create alias")
                }
            }
            Text(
                "You'll be asked to confirm with your PIN or biometrics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DestinationPicker(state: CreateAliasState, onAction: (CreateAliasIntent) -> Unit) {
    when (val options = state.destinations) {
        DestinationOptionsState.Loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        is DestinationOptionsState.Error -> Column {
            Text(options.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onAction(CreateAliasIntent.RetryDestinations) }) { Text("Retry") }
        }
        is DestinationOptionsState.Loaded -> if (options.verified.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No verified destination address", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (options.pendingCount > 0)
                            "${options.pendingCount} address(es) are waiting for verification. Click the link Cloudflare emailed, then tap refresh."
                        else "Add the inbox this alias should forward to. Cloudflare will email it a verification link.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = { onAction(CreateAliasIntent.OpenAddresses) }) { Text("Manage addresses") }
                }
            }
        } else {
            Column(Modifier.selectableGroup()) {
                options.verified.forEach { email ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = state.selectedDestination == email,
                                onClick = { onAction(CreateAliasIntent.SelectDestination(email)) },
                                enabled = !state.saving,
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.selectedDestination == email, onClick = null, enabled = !state.saving)
                        Spacer(Modifier.width(8.dp))
                        Text(email, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.destinationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (options.pendingCount > 0) {
                    Text(
                        "${options.pendingCount} more address(es) are pending verification and can't be used yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onAction(CreateAliasIntent.OpenAddresses) }) { Text("Manage addresses") }
            }
        }
    }
}
