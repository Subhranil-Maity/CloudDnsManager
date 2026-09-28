package com.subhranil.clouddnsmanager.email.aliases.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.email.EmailDataEvents
import com.subhranil.clouddnsmanager.email.api.emailRoutingApi
import com.subhranil.clouddnsmanager.email.domain.EmailValidators
import com.subhranil.clouddnsmanager.email.domain.RandomAliasGenerator
import com.subhranil.clouddnsmanager.email.domain.newForwardRule
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.security.AuthGate
import com.subhranil.clouddnsmanager.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Full-screen form for a new alias: `<local part>@zone` forwarded to one verified address. */
class CreateAliasViewModel(
    private val destination: EmailDestination.CreateAlias,
    private val router: NavigationRouter,
    private val sessionManager: SessionManager,
    private val authGate: AuthGate,
    private val events: EmailDataEvents,
    private val generator: RandomAliasGenerator,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateAliasState(zoneName = destination.zoneName))
    val state = _state.asStateFlow()

    private var destinationsJob: Job? = null

    init {
        loadDestinations()
        // Coming back from the Addresses screen: a newly verified address should show up
        viewModelScope.launch { events.addressesChanged.collect { loadDestinations() } }
    }

    fun onAction(intent: CreateAliasIntent) {
        when (intent) {
            is CreateAliasIntent.UpdateLocalPart -> _state.update {
                it.copy(localPart = intent.value, localPartError = null, saveError = null)
            }
            CreateAliasIntent.GenerateRandom -> _state.update {
                it.copy(localPart = generator.generate(), localPartError = null, saveError = null)
            }
            is CreateAliasIntent.UpdateName -> _state.update { it.copy(name = intent.value) }
            is CreateAliasIntent.SelectDestination -> _state.update {
                it.copy(selectedDestination = intent.email, destinationError = null)
            }
            CreateAliasIntent.RetryDestinations -> loadDestinations()
            CreateAliasIntent.OpenAddresses -> router.push(
                EmailDestination.Addresses(destination.zoneId, destination.zoneName, destination.accountId)
            )
            CreateAliasIntent.Save -> save()
            CreateAliasIntent.Back -> if (!_state.value.saving) router.pop()
        }
    }

    private fun loadDestinations() {
        destinationsJob?.cancel()
        destinationsJob = viewModelScope.launch {
            _state.update { it.copy(destinations = DestinationOptionsState.Loading) }
            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(destinations = DestinationOptionsState.Error("Session expired. Please log in again.")) }
                return@launch
            }
            try {
                val addresses = api.listAddresses(destination.accountId)
                val verified = addresses.filter { it.isVerified }.map { it.email }.sortedBy { it.lowercase() }
                _state.update { s ->
                    s.copy(
                        destinations = DestinationOptionsState.Loaded(verified, addresses.count { !it.isVerified }),
                        // Keep the choice if still valid; pre-select when there is only one option
                        selectedDestination = s.selectedDestination?.takeIf { it in verified } ?: verified.singleOrNull(),
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update {
                    it.copy(destinations = DestinationOptionsState.Error(e.toUserMessage(permissionHint = "Email Routing Addresses: Read")))
                }
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.saving) return
        val localPart = EmailValidators.validateLocalPart(current.localPart, destination.zoneName)
        val chosen = current.selectedDestination
        val options = current.destinations as? DestinationOptionsState.Loaded
        val destinationError = when {
            chosen == null -> "Choose where to forward mail."
            options != null && chosen !in options.verified -> "That address isn't verified yet."
            else -> null
        }
        if (!localPart.isValid || destinationError != null) {
            _state.update { it.copy(localPartError = localPart.error, destinationError = destinationError) }
            return
        }
        val address = "${localPart.value}@${destination.zoneName}"

        viewModelScope.launch {
            if (!authGate.authorize("Create alias $address")) return@launch
            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(saveError = "Session expired. Please log in again.") }
                return@launch
            }
            _state.update { it.copy(saving = true, saveError = null) }
            try {
                api.createRule(destination.zoneId, newForwardRule(address, chosen!!, current.name))
                events.notifyAliasesChanged()
                router.pop()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update {
                    it.copy(saving = false, saveError = e.toUserMessage(permissionHint = "Email Routing Rules: Edit"))
                }
            }
        }
    }
}
