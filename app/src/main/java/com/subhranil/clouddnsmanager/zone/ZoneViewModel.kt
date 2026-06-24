package com.subhranil.clouddnsmanager.zone

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.PaginationParams
import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.selectzones.SelectZoneIntent
import com.subhranil.clouddnsmanager.storage.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections.emptyList

class ZoneViewModel(
    private val router: NavigationRouter,
    private val dataStore: DataStore<UserPreferences>
): ViewModel(){
    private val _state = MutableStateFlow(ZoneState())
    var state = _state.asStateFlow()
    private var client = CloudflareClient("")
    init {
        viewModelScope.launch {
            val token = dataStore.data.first().token ?: ""
            if (token.isEmpty()) {
                router.resetWithStack(listOf(NavDestinations.StartScreenDestination))
                return@launch
            }

            client = CloudflareClient(token)
            try {
                val list: MutableList<DnsRecord> = mutableListOf()
                val zoneId = (router.currentDestination as NavDestinations.ZoneDetailsDestination).zoneId
                val result = client.listDnsRecords(zoneId)
                Log.d("Zone Details", "First Result: ${result.toString()}")
                list.addAll(result.items)
                val total  = result.info?.totalPages
                if (total != null) {
                    for (i in 2..total) {
                        val page = PaginationParams(
                            page = i
                        )
                        val r = client.listDnsRecords(zoneId, pagination = page)
                        list.addAll(r.items)

                    }
                }
                Log.d("Zone Details", "List: $list")

                _state.update {
                    it.copy(dnsRecords = list)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
            _state.update {
                it.copy(isLoading = false)
            }
        }
    }
    public fun onAction(intent: ZoneIntent){
        when (intent) {
            is ZoneIntent.DismissError -> dismissError()
        }
    }

    fun dismissError() {
        _state.update {
            it.copy(error = null)
        }
    }
}