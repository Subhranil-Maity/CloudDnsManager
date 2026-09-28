package com.subhranil.clouddnsmanager.nav

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.subhranil.clouddnsmanager.onboading.OnBoardingScreen
import com.subhranil.clouddnsmanager.selectzones.SelectZoneScreen
import com.subhranil.clouddnsmanager.start.StartScreen
import com.subhranil.clouddnsmanager.dns.nav.dnsEntry
import com.subhranil.clouddnsmanager.dns.nav.registerDnsDestinations
import com.subhranil.clouddnsmanager.email.nav.emailEntry
import com.subhranil.clouddnsmanager.email.nav.registerEmailDestinations
import com.subhranil.clouddnsmanager.zone.ZoneHubScreen
import com.subhranil.clouddnsmanager.security.settings.SecuritySettingsScreen
import com.subhranil.clouddnsmanager.security.setup.PinSetupScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import kotlin.collections.listOf

@Composable
fun RootNavigation(
    modifier: Modifier = Modifier,
    navRouter: NavigationRouter = koinInject()
) {
    val currentStackState by navRouter.navigationState.collectAsState()
    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(
                        NavDestinations.StartScreenDestination::class,
                        NavDestinations.StartScreenDestination.serializer()
                    )
                    subclass(
                        NavDestinations.SelectZonesDestination::class,
                        NavDestinations.SelectZonesDestination.serializer()
                    )
                    subclass(
                        NavDestinations.ZoneHub::class,
                        NavDestinations.ZoneHub.serializer()
                    )
                    subclass(
                        NavDestinations.OnBoarding::class,
                        NavDestinations.OnBoarding.serializer()
                    )
                    subclass(
                        NavDestinations.SecuritySettings::class,
                        NavDestinations.SecuritySettings.serializer()
                    )
                    subclass(
                        NavDestinations.PinSetup::class,
                        NavDestinations.PinSetup.serializer()
                    )
                    // Feature packages register their own destinations
                    registerDnsDestinations()
                    registerEmailDestinations()
                }
            }
        },
        NavDestinations.StartScreenDestination
    )
    // // Example: Moving from OnBoarding to SelectZones and ensuring OnBoarding is gone
    //backStack.set(backStack.entries.dropLast(1) + NavDestinations.SelectZonesDestination)
    LaunchedEffect(currentStackState) {
        Log.d("RootNav", "BackStack Changed ${currentStackState.toString()}")
        // If your Navigation 3 artifact treats backStack directly as the state wrapper:
        if (backStack != currentStackState) {
            backStack.clear()
            backStack.addAll(currentStackState)
        }
    }
    NavDisplay(
        backStack,
        modifier,
        entryDecorators = listOf(
            rememberViewModelStoreNavEntryDecorator(),
            rememberSaveableStateHolderNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is NavDestinations.StartScreenDestination -> NavEntry(key) { StartScreen() }
                is NavDestinations.OnBoarding -> NavEntry(key) { OnBoardingScreen() }
                is NavDestinations.SelectZonesDestination -> NavEntry(key) { SelectZoneScreen() }
                is NavDestinations.ZoneHub -> NavEntry(key) { ZoneHubScreen(destination = key) }
                is NavDestinations.SecuritySettings -> NavEntry(key) { SecuritySettingsScreen() }
                is NavDestinations.PinSetup -> NavEntry(key) { PinSetupScreen(changing = key.changing) }
                else -> dnsEntry(key)
                    ?: emailEntry(key)
                    ?: error("Unsupported navigation destination: $key")
            }
        }
    )

}