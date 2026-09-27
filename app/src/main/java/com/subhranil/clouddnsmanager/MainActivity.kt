package com.subhranil.clouddnsmanager

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.fragment.app.FragmentActivity
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.subhranil.clouddnsmanager.lock.LockScreen
import com.subhranil.clouddnsmanager.security.AppLockManager
import com.subhranil.clouddnsmanager.security.AppLockRepository
import com.subhranil.clouddnsmanager.security.AuthGateHost
import com.subhranil.clouddnsmanager.security.LockState
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


// FragmentActivity (rather than ComponentActivity) is required by BiometricPrompt
class MainActivity : FragmentActivity() {

    val sessionManager: SessionManager by inject<SessionManager>()
    private val router: NavigationRouter by inject()
    private val appLockManager: AppLockManager by inject()
    private val appLockRepository: AppLockRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: swaps the launch theme for the app theme
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the branded splash up while startup state resolves, instead of flashing a
        // placeholder screen. Capped so a stuck startup can never freeze on the splash.
        val splashStart = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            !isReadyToShowContent() && SystemClock.uptimeMillis() - splashStart < MAX_SPLASH_MS
        }

        enableEdgeToEdge()

        // Initialize persistent session state on launch
        lifecycleScope.launch {
            sessionManager.initialize()
        }

        setContent {
            CloudDnsManagerTheme {
                val sessionState by sessionManager.sessionState.collectAsStateWithLifecycle()
                val lockState by appLockManager.lockState.collectAsStateWithLifecycle()
                val isPinSet by appLockRepository.isPinSet.collectAsStateWithLifecycle(initialValue = false)

                // Keep DNS data out of the recent-apps thumbnail and screenshots while a PIN protects the app
                LaunchedEffect(isPinSet) {
                    if (isPinSet) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

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
                when {
                    sessionState is SessionState.Loading || lockState is LockState.Checking -> {
                        // Normally hidden behind the system splash screen (see isReadyToShowContent).
                        // Only visible if startup is unusually slow and the splash timeout passes.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    else -> {
                        val locked = lockState is LockState.Locked
                        Box(Modifier.fillMaxSize()) {
                            // Navigation stays composed while locked so the back stack and
                            // ViewModels survive; it's just hidden from accessibility services.
                            Scaffold(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (locked) Modifier.clearAndSetSemantics { } else Modifier)
                            ) { innerPadding ->
                                RootNavigation(modifier = Modifier.padding(innerPadding))
                            }
                            if (locked) {
                                LockScreen()
                            }
                        }
                    }
                }

                // Renders PIN / biometric prompts requested via AuthGate.authorize()
                AuthGateHost()
            }
        }
    }

    /**
     * True once the session and app-lock state are known and the router has left the
     * placeholder start destination, i.e. the first real screen (or lock screen) can be drawn.
     */
    private fun isReadyToShowContent(): Boolean =
        sessionManager.sessionState.value !is SessionState.Loading &&
            appLockManager.lockState.value !is LockState.Checking &&
            router.navigationState.value.lastOrNull() !is NavDestinations.StartScreenDestination

    private companion object {
        const val MAX_SPLASH_MS = 3_000L
    }
}