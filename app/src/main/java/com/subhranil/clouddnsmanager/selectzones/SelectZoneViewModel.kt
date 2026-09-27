package com.subhranil.clouddnsmanager.selectzones

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.models.zone.Zone
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.security.AuthGate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectZoneViewModel(
    private val router: NavigationRouter,
    private val sessionManager: SessionManager,
    private val authGate: AuthGate
) : ViewModel() {

    private val _state = MutableStateFlow(SelectZoneState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadZones()
    }

    private fun loadZones() {
        // Cancel any in-flight load so rapid Retry taps don't run parallel collectors
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(dataState = SelectZoneDataState.Loading) }

            val client = sessionManager.clientOrNull()
            if (client == null) {
                _state.update {
                    it.copy(dataState = SelectZoneDataState.Error("Session expired. Please log in again."))
                }
                return@launch
            }

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
            is SelectZoneIntent.RequestLogout -> _state.update { it.copy(showLogoutConfirmation = true) }
            is SelectZoneIntent.DismissLogout -> _state.update { it.copy(showLogoutConfirmation = false) }
            is SelectZoneIntent.ConfirmLogout -> logout()
            is SelectZoneIntent.OpenSecurity -> router.push(NavDestinations.SecuritySettings)
        }
    }

    private fun selectZone(zoneId: String) {
        Log.d("Select Zone Screen", "Pushing To Stack")
        router.push(NavDestinations.DnsRecordsDestination(zoneId))
    }

    private fun logout() {
        _state.update { it.copy(showLogoutConfirmation = false) }
        // Security rule: destructive actions must pass biometric / PIN auth (see AuthGate).
        // MainActivity observes the Unauthenticated state and resets the stack to OnBoarding.
        viewModelScope.launch {
            if (authGate.authorize("Log out of Dns Manager")) sessionManager.logout()
        }
    }
}
