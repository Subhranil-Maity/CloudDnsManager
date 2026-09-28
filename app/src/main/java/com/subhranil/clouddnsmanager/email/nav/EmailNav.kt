package com.subhranil.clouddnsmanager.email.nav

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.subhranil.clouddnsmanager.email.home.EmailHomeScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Every Email screen's navigation key. Owned by the Email feature: add new screens here
 * (plus a line in [registerEmailDestinations] and [emailEntry]) without touching nav/.
 */
@Serializable
sealed interface EmailDestination : NavKey {
    /** [accountId] is needed for destination addresses, which are account-level in Cloudflare. */
    @Serializable
    data class Home(
        val zoneId: String,
        val zoneName: String,
        val accountId: String,
    ) : EmailDestination
}

/** Called from RootNavigation's SerializersModule so the back stack can be saved. */
fun PolymorphicModuleBuilder<NavKey>.registerEmailDestinations() {
    subclass(EmailDestination.Home::class, EmailDestination.Home.serializer())
}

/** Called from RootNavigation's entryProvider; returns null for keys this feature doesn't own. */
fun emailEntry(key: NavKey): NavEntry<NavKey>? = when (key) {
    is EmailDestination.Home -> NavEntry(key) { EmailHomeScreen(destination = key) }
    else -> null
}
