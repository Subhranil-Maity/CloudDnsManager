package com.subhranil.clouddnsmanager.email.aliases.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.email.EmailDataEvents
import com.subhranil.clouddnsmanager.email.api.emailRoutingApi
import com.subhranil.clouddnsmanager.email.domain.AliasTarget
import com.subhranil.clouddnsmanager.email.domain.toAliasDisplay
import com.subhranil.clouddnsmanager.email.domain.toUpdateRequest
import com.subhranil.clouddnsmanager.email.model.EmailRoutingRule
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.localstore.ItemKey
import com.subhranil.clouddnsmanager.localstore.lock.ItemLockManager
import com.subhranil.clouddnsmanager.localstore.lock.LockResult
import com.subhranil.clouddnsmanager.localstore.notes.NoteRepository
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.security.AuthGate
import com.subhranil.clouddnsmanager.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RULES_EDIT = "Email Routing Rules: Edit"

/** One alias: enable/disable, change destination or name, delete, local lock and note. */
class AliasDetailViewModel(
    private val destination: EmailDestination.AliasDetail,
    private val router: NavigationRouter,
    private val sessionManager: SessionManager,
    private val authGate: AuthGate,
    private val lockManager: ItemLockManager,
    private val noteRepository: NoteRepository,
    private val events: EmailDataEvents,
) : ViewModel() {

    private val _state = MutableStateFlow(AliasDetailState(zoneName = destination.zoneName))
    val state = _state.asStateFlow()

    private val itemKey = ItemKey.emailRule(destination.zoneId, destination.ruleId)
    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch { lockManager.observeLocked(itemKey).collect { l -> _state.update { it.copy(locked = l) } } }
        viewModelScope.launch { noteRepository.observeNote(itemKey).collect { n -> _state.update { it.copy(note = n) } } }
        viewModelScope.launch { events.addressesChanged.collect { loadDestinations() } }
    }

    fun onAction(intent: AliasDetailIntent) {
        when (intent) {
            AliasDetailIntent.Retry -> load()
            AliasDetailIntent.Back -> if (!_state.value.busy) router.pop()
            is AliasDetailIntent.SetEnabled -> setEnabled(intent.enabled)
            AliasDetailIntent.StartEdit -> startEdit()
            AliasDetailIntent.CancelEdit -> _state.update { if (it.edit?.saving == true) it else it.copy(edit = null) }
            is AliasDetailIntent.UpdateEditName -> _state.update { s -> s.copy(edit = s.edit?.copy(name = intent.name, error = null)) }
            is AliasDetailIntent.SelectEditDestination -> _state.update { s ->
                s.copy(edit = s.edit?.copy(destination = intent.email, error = null))
            }
            AliasDetailIntent.SaveEdit -> saveEdit()
            AliasDetailIntent.RequestDelete -> viewModelScope.launch {
                if (ensureUnlocked()) _state.update { it.copy(confirmDelete = true) }
            }
            AliasDetailIntent.ConfirmDelete -> delete()
            AliasDetailIntent.DismissDelete -> _state.update { it.copy(confirmDelete = false) }
            is AliasDetailIntent.SetLocked -> setLocked(intent.locked)
            is AliasDetailIntent.SaveNote -> viewModelScope.launch {
                noteRepository.setNote(itemKey, intent.note)
                _state.update { it.copy(message = if (intent.note.isBlank()) "Note removed." else "Note saved on this device.") }
            }
            AliasDetailIntent.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(dataState = AliasDetailDataState.Loading) }
            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(dataState = AliasDetailDataState.Error("Session expired. Please log in again.")) }
                return@launch
            }
            try {
                val rule = api.getRule(destination.zoneId, destination.ruleId)
                setRule(rule)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update {
                    it.copy(dataState = AliasDetailDataState.Error(e.toUserMessage(permissionHint = "Email Routing Rules: Read")))
                }
                return@launch
            }
            loadDestinations()
        }
    }

    /** Verified addresses for the edit picker. Failure only hides the picker's extra options. */
    private fun loadDestinations() {
        viewModelScope.launch {
            val api = sessionManager.clientOrNull()?.emailRoutingApi() ?: return@launch
            val verified = try {
                api.listAddresses(destination.accountId).filter { it.isVerified }.map { it.email }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }
            _state.update { it.copy(verifiedDestinations = verified) }
        }
    }

    private fun setRule(rule: EmailRoutingRule) {
        // Keep the id we navigated with if the response leaves it out
        val fixed = if (rule.ruleId.isBlank()) rule.copy(id = destination.ruleId) else rule
        _state.update { it.copy(dataState = AliasDetailDataState.Loaded(fixed, fixed.toAliasDisplay())) }
    }

    private fun loadedRule(): EmailRoutingRule? = (_state.value.dataState as? AliasDetailDataState.Loaded)?.rule

    /** True when editable; otherwise tells the user to unlock first. */
    private suspend fun ensureUnlocked(): Boolean {
        if (!lockManager.isLocked(itemKey)) return true
        _state.update { it.copy(message = "This alias is locked. Unlock it first.") }
        return false
    }

    private fun setEnabled(enabled: Boolean) {
        val rule = loadedRule() ?: return
        if (_state.value.busy) return
        viewModelScope.launch {
            if (!ensureUnlocked()) return@launch
            val title = rule.toAliasDisplay().title
            if (!authGate.authorize("${if (enabled) "Enable" else "Disable"} alias $title")) return@launch
            val api = sessionManager.clientOrNull()?.emailRoutingApi() ?: return@launch sessionExpired()
            _state.update { it.copy(togglingEnabled = true) }
            try {
                val updated = api.updateRule(destination.zoneId, rule.ruleId, rule.toUpdateRequest(enabled = enabled))
                setRule(if (updated.ruleId.isBlank()) rule.copy(enabled = enabled) else updated)
                events.notifyAliasesChanged()
                _state.update { it.copy(message = if (enabled) "Alias enabled." else "Alias disabled.") }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(message = e.toUserMessage(permissionHint = RULES_EDIT)) }
            } finally {
                _state.update { it.copy(togglingEnabled = false) }
            }
        }
    }

    private fun startEdit() {
        val rule = loadedRule() ?: return
        viewModelScope.launch {
            if (!ensureUnlocked()) return@launch
            val target = rule.toAliasDisplay().target
            _state.update {
                it.copy(
                    edit = AliasEditForm(
                        name = rule.name.orEmpty(),
                        destination = (target as? AliasTarget.Forward)?.destinations?.firstOrNull()
                            ?: if (target is AliasTarget.Forward) "" else null,
                    )
                )
            }
        }
    }

    private fun saveEdit() {
        val rule = loadedRule() ?: return
        val form = _state.value.edit ?: return
        if (form.saving) return
        val display = rule.toAliasDisplay()
        val newDestination = form.destination
        if (newDestination != null && newDestination.isBlank()) {
            _state.update { it.copy(edit = form.copy(error = "Choose where to forward mail.")) }
            return
        }
        // Only replace the action when the destination actually changed (keeps multi-value forwards intact)
        val currentDestinations = (display.target as? AliasTarget.Forward)?.destinations.orEmpty()
        val forwardTo = newDestination?.takeIf { currentDestinations != listOf(it) }
        val newName = form.name.trim()
        if (forwardTo == null && newName == rule.name.orEmpty().trim()) {
            _state.update { it.copy(edit = null) }
            return
        }
        viewModelScope.launch {
            if (!ensureUnlocked()) return@launch
            if (!authGate.authorize("Edit alias ${display.title}")) return@launch
            val api = sessionManager.clientOrNull()?.emailRoutingApi() ?: return@launch sessionExpired()
            _state.update { it.copy(edit = form.copy(saving = true, error = null)) }
            try {
                val request = rule.toUpdateRequest(name = newName, forwardTo = forwardTo)
                val updated = api.updateRule(destination.zoneId, rule.ruleId, request)
                setRule(
                    if (updated.ruleId.isBlank()) rule.copy(name = request.name, actions = request.actions) else updated
                )
                events.notifyAliasesChanged()
                _state.update { it.copy(edit = null, message = "Alias updated.") }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(edit = form.copy(saving = false, error = e.toUserMessage(permissionHint = RULES_EDIT))) }
            }
        }
    }

    private fun delete() {
        val rule = loadedRule() ?: return
        _state.update { it.copy(confirmDelete = false) }
        viewModelScope.launch {
            // Checked again: the lock could have changed while the dialog was open
            if (!ensureUnlocked()) return@launch
            if (!authGate.authorize("Delete alias ${rule.toAliasDisplay().title}")) return@launch
            val api = sessionManager.clientOrNull()?.emailRoutingApi() ?: return@launch sessionExpired()
            _state.update { it.copy(deleting = true) }
            try {
                api.deleteRule(destination.zoneId, rule.ruleId)
                noteRepository.deleteNote(itemKey)
                lockManager.forget(itemKey)
                events.notifyAliasesChanged()
                router.pop()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(deleting = false, message = e.toUserMessage(permissionHint = RULES_EDIT)) }
            }
        }
    }

    private fun setLocked(locked: Boolean) {
        viewModelScope.launch {
            // lock() needs no auth by design; unlock() runs the AuthGate itself (never authorize twice)
            val result = if (locked) lockManager.lock(itemKey) else lockManager.unlock(itemKey, "Unlock alias")
            when (result) {
                LockResult.Success -> if (locked) _state.update { it.copy(edit = null) }
                LockResult.NotAuthorized -> _state.update { it.copy(message = "Still locked: authentication was cancelled.") }
                is LockResult.Failed -> _state.update { it.copy(message = result.error.toUserMessage()) }
            }
        }
    }

    private fun sessionExpired() {
        _state.update { it.copy(message = "Session expired. Please log in again.") }
    }
}
