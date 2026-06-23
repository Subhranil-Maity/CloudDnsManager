package com.subhranil.clouddnsmanager.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
sealed interface NavDestinations: NavKey {
    @Serializable data object StartScreenDestination: NavDestinations
    @Serializable data object SelectZonesDestination: NavDestinations
    @Serializable data class ZoneDetailsDestination(val zoneId: String): NavDestinations
    @Serializable data object OnBoarding: NavDestinations
}