package com.subhranil.clouddnsmanager.selectzones

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.models.zone.Zone
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectZoneViewModel(
    private val router: NavigationRouter,
    private val client: CloudflareClient
) : ViewModel() {

    private val _state = MutableStateFlow(SelectZoneState())
    val state = _state.asStateFlow()

    init {
        loadZones()
    }

    private fun loadZones() {
        viewModelScope.launch {
            _state.update { it.copy(dataState = SelectZoneDataState.Loading) }

            val accumulatedZones = mutableListOf<Zone>()

            client.allZones()
                .catch { exception ->
                    _state.update {
                        it.copy(
                            dataState = SelectZoneDataState.Error(
                                exception.message ?: "Unknown Error"
                            )
                        )
                    }
                }
                .collect { singleZone ->
                    accumulatedZones.add(singleZone)

                    // Emitting a clean list snapshot as each individual zone loads sequentially
                    _state.update {
                        it.copy(dataState = SelectZoneDataState.ZoneData(accumulatedZones.toList()))
                    }
                }
        }
    }

    fun onAction(intent: SelectZoneIntent) {
        when (intent) {
            is SelectZoneIntent.SelectZone -> selectZone(intent.zoneId)
            is SelectZoneIntent.Retry -> loadZones()
//            is SelectZoneIntent.DismissError -> dismissError()
        }
    }

    private fun selectZone(zoneId: String) {
        Log.d("Select Zone Screen", "Pushing To Stack")
        router.push(NavDestinations.DnsRecordsDestination(zoneId))
    }

    private fun dismissError() {
        // Fall back gracefully to displaying an empty or previous zone data state
        _state.update { it.copy(dataState = SelectZoneDataState.ZoneData(emptyList())) }
    }
}