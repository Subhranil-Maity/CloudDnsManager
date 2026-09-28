package com.subhranil.clouddnsmanager.email.nav

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.subhranil.clouddnsmanager.email.addresses.AddressesScreen
import com.subhranil.clouddnsmanager.email.aliases.create.CreateAliasScreen
import com.subhranil.clouddnsmanager.email.aliases.detail.AliasDetailScreen
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

    /** Full-screen form for a new alias in [zoneName]. */
    @Serializable
    data class CreateAlias(
        val zoneId: String,
        val zoneName: String,
        val accountId: String,
    ) : EmailDestination

    /** Details, edit, lock, note and activity for one alias (routing rule). */
    @Serializable
    data class AliasDetail(
        val zoneId: String,
        val zoneName: String,
        val accountId: String,
        val ruleId: String,
    ) : EmailDestination

    /** Stand-alone destination address list, opened from the Create Alias form. */
    @Serializable
    data class Addresses(
        val zoneId: String,
        val zoneName: String,
        val accountId: String,
    ) : EmailDestination
}

/** Called from RootNavigation's SerializersModule so the back stack can be saved. */
fun PolymorphicModuleBuilder<NavKey>.registerEmailDestinations() {
    subclass(EmailDestination.Home::class, EmailDestination.Home.serializer())
    subclass(EmailDestination.CreateAlias::class, EmailDestination.CreateAlias.serializer())
    subclass(EmailDestination.AliasDetail::class, EmailDestination.AliasDetail.serializer())
    subclass(EmailDestination.Addresses::class, EmailDestination.Addresses.serializer())
}

/** Called from RootNavigation's entryProvider; returns null for keys this feature doesn't own. */
fun emailEntry(key: NavKey): NavEntry<NavKey>? = when (key) {
    is EmailDestination.Home -> NavEntry(key) { EmailHomeScreen(destination = key) }
    is EmailDestination.CreateAlias -> NavEntry(key) { CreateAliasScreen(destination = key) }
    is EmailDestination.AliasDetail -> NavEntry(key) { AliasDetailScreen(destination = key) }
    is EmailDestination.Addresses -> NavEntry(key) { AddressesScreen(destination = key) }
    else -> null
}
