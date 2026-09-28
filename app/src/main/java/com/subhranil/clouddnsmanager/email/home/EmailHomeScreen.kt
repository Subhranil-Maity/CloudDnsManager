package com.subhranil.clouddnsmanager.email.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.email.EmailZone
import com.subhranil.clouddnsmanager.email.activity.ActivitySection
import com.subhranil.clouddnsmanager.email.addresses.AddressesSection
import com.subhranil.clouddnsmanager.email.aliases.AliasListSection
import com.subhranil.clouddnsmanager.email.model.EmailRoutingSettings
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Entry screen of the Email feature: routing status plus Aliases / Addresses / Activity tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailHomeScreen(
    destination: EmailDestination.Home,
    modifier: Modifier = Modifier,
    viewModel: EmailHomeViewModel = koinViewModel { parametersOf(destination) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zone = EmailZone(destination.zoneId, destination.zoneName, destination.accountId)

    BackHandler { viewModel.onAction(EmailHomeIntent.Back) }

    EmailHomeContent(state = state, onAction = viewModel::onAction, modifier = modifier) { tab ->
        // Each tab owns a ViewModel scoped to this screen, so switching tabs keeps its data
        when (tab) {
            EmailTab.Aliases -> AliasListSection(zone)
            EmailTab.Addresses -> AddressesSection(zone)
            EmailTab.Activity -> ActivitySection(zone.zoneId)
        }
    }
}

/**
 * Stateless email hub (also used by previews). [tabContent] renders the selected tab, so the
 * real screen can plug in ViewModel-backed sections and previews can plug in sample content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailHomeContent(
    state: EmailHomeState,
    onAction: (EmailHomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    tabContent: @Composable (EmailTab) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Email", fontWeight = FontWeight.Bold)
                        Text(
                            state.zoneName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(EmailHomeIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            RoutingStatusBanner(state.dataState, onRetry = { onAction(EmailHomeIntent.Retry) })

            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                EmailTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onAction(EmailHomeIntent.SelectTab(tab)) },
                        text = { Text(tab.title) },
                    )
                }
            }

            tabContent(state.selectedTab)
        }
    }
}

/** Short explanation of a non-working status, or null when routing works. */
private fun problemText(settings: EmailRoutingSettings): String? {
    val status = settings.status
    return when {
        !settings.enabled ->
            "Email Routing is turned off for this zone, so aliases won't receive mail. " +
                "Turn it on in the Cloudflare dashboard (Email → Email Routing). You can still prepare aliases and addresses here."
        status == null || status == "ready" || status == "unlocked" -> null
        status.startsWith("misconfigured") ->
            "Email Routing is on but its DNS records are missing or wrong (status: $status). " +
                "Fix them in the Cloudflare dashboard (Email → Email Routing → Settings)."
        status == "unconfigured" ->
            "Email Routing hasn't been set up for this zone yet. Finish the setup in the Cloudflare dashboard (Email → Email Routing)."
        else -> "Email Routing status is \"$status\". Check it in the Cloudflare dashboard."
    }
}

@Composable
private fun RoutingStatusBanner(dataState: EmailHomeDataState, onRetry: () -> Unit) {
    when (dataState) {
        EmailHomeDataState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        // The status is optional extra info that needs "Zone Settings: Read". Without that
        // permission everything else still works, so show nothing rather than a warning.
        EmailHomeDataState.StatusNotPermitted -> Unit
        is EmailHomeDataState.Error -> WarningCard(
            text = "Couldn't read the Email Routing status. ${dataState.message}",
            onRetry = onRetry,
        )
        is EmailHomeDataState.Loaded -> {
            val problem = problemText(dataState.settings)
            if (problem != null) {
                WarningCard(text = problem, onRetry = onRetry)
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF137333), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Email Routing is on", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun WarningCard(text: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                TextButton(onClick = onRetry) { Text("Check again") }
            }
        }
    }
}
