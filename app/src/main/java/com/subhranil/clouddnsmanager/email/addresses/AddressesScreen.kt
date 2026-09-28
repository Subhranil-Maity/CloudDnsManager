package com.subhranil.clouddnsmanager.email.addresses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.subhranil.clouddnsmanager.email.EmailZone
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import org.koin.compose.koinInject

/**
 * Stand-alone destination address list (opened from Create Alias when no address is verified).
 * The list itself is [AddressesSection], with its own ViewModel/State/Intent; this screen only
 * adds a top bar, so Back is plain navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressesScreen(
    destination: EmailDestination.Addresses,
    modifier: Modifier = Modifier,
    router: NavigationRouter = koinInject(),
) {
    BackHandler { router.pop() }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Destination addresses", fontWeight = FontWeight.Bold)
                        Text(
                            destination.zoneName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { router.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        AddressesSection(
            zone = EmailZone(destination.zoneId, destination.zoneName, destination.accountId),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
