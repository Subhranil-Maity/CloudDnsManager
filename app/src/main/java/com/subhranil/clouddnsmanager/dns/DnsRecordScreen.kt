package com.subhranil.clouddnsmanager.dns

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.dns.components.DnsRecordsScreen
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import org.koin.androidx.compose.koinViewModel


@Composable
fun DnsRecordScreen(
    modifier: Modifier = Modifier,
    viewModel: DnsRecordViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // intercept system back handler patterns cleanly
    BackHandler {
        viewModel.onAction(DnsRecordIntent.GoBack)
    }

    // --- Dynamic Content State Resolution ---
    when (val dataState = state.dnsRecordDataState) {
        is DnsRecordDataState.Loading -> {
            // Pass empty list to activate your custom Crossfade shimmering placeholder layout rows
            DnsRecordsScreen(
                dnsRecords = emptyList(),
                isLoading = true,
                drawer = state.openDetailedDrawer,
                onDrawerDismiss = { viewModel.onAction(DnsRecordIntent.DismissDetailedDrawer) },
                onSelectDrawer = { record: DnsRecord -> viewModel.onAction(DnsRecordIntent.ShowDetailed(record)) },
                modifier = modifier
            )
        }

        is DnsRecordDataState.Error -> {
            // Clean, dedicated full screen error layout
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Error icon",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = dataState.error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { viewModel.onAction(DnsRecordIntent.Retry) }
                ) {
                    Text(text = "Retry")
                }
            }
        }

        is DnsRecordDataState.DnsRecordData -> {
            // Render the production active data stream list directly with your detailed sub-drawers
            DnsRecordsScreen(
                dnsRecords = dataState.dnsList,
                isLoading = false,
                drawer = state.openDetailedDrawer,
                onDrawerDismiss = { viewModel.onAction(DnsRecordIntent.DismissDetailedDrawer) },
                onSelectDrawer = { record: DnsRecord -> viewModel.onAction(DnsRecordIntent.ShowDetailed(record)) },
                modifier = modifier
            )
        }
    }
}