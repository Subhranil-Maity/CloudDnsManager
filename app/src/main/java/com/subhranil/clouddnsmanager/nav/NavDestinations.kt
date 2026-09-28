package com.subhranil.clouddnsmanager.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestinations: NavKey {
    @Serializable data object StartScreenDestination: NavDestinations
    @Serializable data object SelectZonesDestination: NavDestinations
    /** Zone page with the DNS / Email buttons. Feature screens live in dns/nav and email/nav. */
    @Serializable data class ZoneHub(
        val zoneId: String,
        val zoneName: String,
        val accountId: String,
    ): NavDestinations
    @Serializable data object OnBoarding: NavDestinations
    @Serializable data object SecuritySettings: NavDestinations
    @Serializable data class PinSetup(val changing: Boolean): NavDestinations
}