package com.subhranil.clouddnsmanager.dns.nav

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.subhranil.clouddnsmanager.dns.DnsRecordScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * Every DNS screen's navigation key. Owned by the DNS feature: add new screens here
 * (plus a line in [registerDnsDestinations] and [dnsEntry]) without touching nav/.
 */
@Serializable
sealed interface DnsDestination : NavKey {
    @Serializable
    data class Records(val zoneId: String) : DnsDestination
}

/** Called from RootNavigation's SerializersModule so the back stack can be saved. */
fun PolymorphicModuleBuilder<NavKey>.registerDnsDestinations() {
    subclass(DnsDestination.Records::class, DnsDestination.Records.serializer())
}

/** Called from RootNavigation's entryProvider; returns null for keys this feature doesn't own. */
fun dnsEntry(key: NavKey): NavEntry<NavKey>? = when (key) {
    is DnsDestination.Records -> NavEntry(key) { DnsRecordScreen(zoneId = key.zoneId) }
    else -> null
}
