package com.subhranil.clouddnsmanager.nav

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.subhranil.clouddnsmanager.onboading.OnBoardingScreen
import com.subhranil.clouddnsmanager.selectzones.SelectZoneScreen
import com.subhranil.clouddnsmanager.start.StartScreen
import com.subhranil.clouddnsmanager.zone.ZoneDetailsScreen
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
                        NavDestinations.ZoneDetailsDestination::class,
                        NavDestinations.ZoneDetailsDestination.serializer()
                    )
                    subclass(
                        NavDestinations.OnBoarding::class,
                        NavDestinations.OnBoarding.serializer()
                    )
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
                is NavDestinations.ZoneDetailsDestination -> NavEntry(key) { ZoneDetailsScreen() }
                else -> error("Unsupported navigation destination: $key")
            }
        }
    )

}