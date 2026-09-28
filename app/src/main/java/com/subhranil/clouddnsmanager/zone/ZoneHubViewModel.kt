package com.subhranil.clouddnsmanager.zone

import androidx.lifecycle.ViewModel
import com.subhranil.clouddnsmanager.dns.nav.DnsDestination
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Zone page: routes into the DNS and Email features for one zone. */
class ZoneHubViewModel(
    private val zone: NavDestinations.ZoneHub,
    private val router: NavigationRouter,
) : ViewModel() {

    private val _state = MutableStateFlow(ZoneHubState(zoneName = zone.zoneName))
    val state = _state.asStateFlow()

    fun onAction(intent: ZoneHubIntent) {
        when (intent) {
            ZoneHubIntent.OpenDns -> router.push(DnsDestination.Records(zone.zoneId))
            ZoneHubIntent.OpenEmail -> router.push(
                EmailDestination.Home(zone.zoneId, zone.zoneName, zone.accountId)
            )
            ZoneHubIntent.Back -> router.pop()
        }
    }
}
