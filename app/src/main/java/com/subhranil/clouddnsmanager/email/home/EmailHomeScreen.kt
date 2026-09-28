package com.subhranil.clouddnsmanager.email.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Entry screen of the Email feature. Placeholder until aliases / addresses / activity land. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailHomeScreen(
    destination: EmailDestination.Home,
    modifier: Modifier = Modifier,
    viewModel: EmailHomeViewModel = koinViewModel { parametersOf(destination) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler { viewModel.onAction(EmailHomeIntent.Back) }

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
                    IconButton(onClick = { viewModel.onAction(EmailHomeIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val data = state.dataState) {
                EmailHomeDataState.Loading -> CircularProgressIndicator()
                is EmailHomeDataState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(data.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.onAction(EmailHomeIntent.Retry) }) { Text("Retry") }
                }
                is EmailHomeDataState.Loaded -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (data.settings.enabled) "Email Routing is on" else "Email Routing is off",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    data.settings.status?.let {
                        Text("Status: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "Aliases, destination addresses and activity are coming soon.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
