package com.subhranil.clouddnsmanager.email.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.email.api.emailRoutingApi
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Email hub: Email Routing status for the zone plus the selected tab. Each tab has its own ViewModel. */
class EmailHomeViewModel(
    private val destination: EmailDestination.Home,
    private val router: NavigationRouter,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(EmailHomeState(zoneName = destination.zoneName))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(intent: EmailHomeIntent) {
        when (intent) {
            EmailHomeIntent.Retry -> load()
            EmailHomeIntent.Back -> router.pop()
            is EmailHomeIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(dataState = EmailHomeDataState.Loading) }
            val client = sessionManager.clientOrNull()
            if (client == null) {
                _state.update { it.copy(dataState = EmailHomeDataState.Error("Session expired. Please log in again.")) }
                return@launch
            }
            try {
                val settings = client.emailRoutingApi().getSettings(destination.zoneId)
                _state.update { it.copy(dataState = EmailHomeDataState.Loaded(settings)) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update {
                    it.copy(dataState = EmailHomeDataState.Error(e.toUserMessage(permissionHint = "Email Routing Rules: Read")))
                }
            }
        }
    }
}
