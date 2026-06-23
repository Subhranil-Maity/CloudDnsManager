package com.subhranil.clouddnsmanager.selectzones

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.PaginationParams
import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.models.zone.Zone
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.storage.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class SelectZoneViewModel(
    private val router: NavigationRouter,
    private val dataStore: DataStore<UserPreferences>
) : ViewModel() {
    private val _state = MutableStateFlow(SelectZoneState())
    val state = _state.asStateFlow()
    var client = CloudflareClient("")


    init {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true)
            }
            val token = dataStore.data.first().token ?: ""
            if (token.isEmpty()) {
                router.resetWithStack(listOf(NavDestinations.StartScreenDestination))
            }
            client = CloudflareClient(token)
            try {
                var zone: MutableList<Zone> = mutableListOf<Zone>()
                val pZ = client.listZones()
                zone.addAll(pZ.items)
                if (pZ.info != null) {
                    val totalPage = pZ.info.totalPages
                    for (i in 2..totalPage) {
                        zone.addAll(client.listZones(pagination = PaginationParams(page = i)).items)
                    }
                }
                _state.update {
                    it.copy(zones = zone)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message)
                }
            }
            _state.update {
                it.copy(loading = true)
            }
        }

    }
    public fun onAction(intent: SelectZoneIntent){
        when (intent) {
            is SelectZoneIntent.DismissError -> dismissError()
        }
    }

    fun dismissError() {
        _state.update {
            it.copy(error = null)
        }
    }
}