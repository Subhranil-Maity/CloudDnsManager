package com.subhranil.clouddnsmanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.http.SessionState
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.nav.RootNavigation
import com.subhranil.clouddnsmanager.storage.UserPreferencesSerializer
import com.subhranil.clouddnsmanager.ui.theme.CloudDnsManagerTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject


class MainActivity : ComponentActivity() {

    val sessionManager: SessionManager by inject<SessionManager>()
    private val router: NavigationRouter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize persistent session state on launch
        lifecycleScope.launch {
            sessionManager.initialize()
        }

        setContent {
            CloudDnsManagerTheme {
                val sessionState by sessionManager.sessionState.collectAsStateWithLifecycle()

                // --- Centralized Routing Effects Engine ---
                LaunchedEffect(sessionState) {
                    when (sessionState) {
                        is SessionState.Authenticated -> {
                            // Completely purge onboarding/splash from history and drop into the core dashboard
                            router.resetWithStack(listOf(NavDestinations.SelectZonesDestination))
                        }
                        is SessionState.Unauthenticated -> {
                            // Clear history and kick user directly into the token input workflow
                            router.resetWithStack(listOf(NavDestinations.OnBoarding))
                        }
                        is SessionState.Loading -> {
                            // Do nothing, wait for initializing state resolution
                        }
                    }
                }

                // --- UI Renderer ---
                when (sessionState) {
                    is SessionState.Loading -> {
                        // Clean layout showing loading text under the indicator
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Checking session status...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                        // Safe to pass execution down to the core layout framework
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            RootNavigation(modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}