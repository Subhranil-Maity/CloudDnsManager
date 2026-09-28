package com.subhranil.clouddnsmanager.email.aliases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.email.EmailDataEvents
import com.subhranil.clouddnsmanager.email.EmailZone
import com.subhranil.clouddnsmanager.email.api.emailRoutingApi
import com.subhranil.clouddnsmanager.email.domain.toAliasDisplay
import com.subhranil.clouddnsmanager.email.domain.toUpdateRequest
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.localstore.ItemKey
import com.subhranil.clouddnsmanager.localstore.lock.ItemLockManager
import com.subhranil.clouddnsmanager.localstore.notes.NoteRepository
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.security.AuthGate
import com.subhranil.clouddnsmanager.toUserMessage
import com.subhranil.clouddnsmanager.email.model.EmailRoutingRule
import com.subhranil.clouddnsmanager.email.model.EmailRuleTypes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The Aliases tab: every routing rule in the zone, with search and an enable switch. */
class AliasListViewModel(
    private val zone: EmailZone,
    private val router: NavigationRouter,
    private val sessionManager: SessionManager,
    private val authGate: AuthGate,
    private val lockManager: ItemLockManager,
    noteRepository: NoteRepository,
    events: EmailDataEvents,
) : ViewModel() {

    private val _state = MutableStateFlow(AliasListState(zoneName = zone.zoneName))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private val keyPrefix = ItemKey.emailRulePrefix(zone.zoneId)

    init {
        load()
        viewModelScope.launch {
            lockManager.observeLockedItems(keyPrefix).collect { keys ->
                _state.update { it.copy(lockedRuleIds = keys.map { key -> key.value.removePrefix(keyPrefix) }.toSet()) }
            }
        }
        viewModelScope.launch {
            noteRepository.observeNotes(keyPrefix).collect { notes ->
                _state.update { it.copy(notedRuleIds = notes.keys.map { key -> key.value.removePrefix(keyPrefix) }.toSet()) }
            }
        }
        viewModelScope.launch { events.aliasesChanged.collect { load(showSpinner = false) } }
    }

    fun onAction(intent: AliasListIntent) {
        when (intent) {
            AliasListIntent.Retry -> load()
            is AliasListIntent.Search -> _state.update { it.copy(query = intent.query) }
            is AliasListIntent.SetEnabled -> setEnabled(intent.ruleId, intent.enabled)
            is AliasListIntent.Open -> router.push(
                EmailDestination.AliasDetail(zone.zoneId, zone.zoneName, zone.accountId, intent.ruleId)
            )
            AliasListIntent.Create -> router.push(
                EmailDestination.CreateAlias(zone.zoneId, zone.zoneName, zone.accountId)
            )
            AliasListIntent.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun load(showSpinner: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showSpinner) _state.update { it.copy(dataState = AliasListDataState.Loading) }
            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(dataState = AliasListDataState.Error("Session expired. Please log in again.")) }
                return@launch
            }
            try {
                // The catch-all is optional extra info; its failure must not hide the aliases.
                val catchAll = async { runCatching { api.getCatchAll(zone.zoneId) }.getOrNull() }
                val rules = api.listRules(zone.zoneId)
                    .filterNot { rule -> rule.matchers.any { it.type == EmailRuleTypes.MATCHER_ALL } }
                    .sortedWith(compareBy({ it.priority ?: Int.MAX_VALUE }, { it.toAliasDisplay().title }))
                val rows = rules.map { AliasRow(it, it.toAliasDisplay()) }
                _state.update {
                    it.copy(dataState = AliasListDataState.Loaded(rows, catchAll.await()?.toAliasDisplay()))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update {
                    it.copy(dataState = AliasListDataState.Error(e.toUserMessage(permissionHint = "Email Routing Rules: Read")))
                }
            }
        }
    }

    private fun setEnabled(ruleId: String, enabled: Boolean) {
        val loaded = _state.value.dataState as? AliasListDataState.Loaded ?: return
        val row = loaded.aliases.firstOrNull { it.rule.ruleId == ruleId } ?: return
        if (ruleId in _state.value.busyRuleIds) return

        viewModelScope.launch {
            if (lockManager.isLocked(ItemKey.emailRule(zone.zoneId, ruleId))) {
                _state.update { it.copy(message = "${row.display.title} is locked. Unlock it first.") }
                return@launch
            }
            val verb = if (enabled) "Enable" else "Disable"
            if (!authGate.authorize("$verb alias ${row.display.title}")) return@launch

            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(message = "Session expired. Please log in again.") }
                return@launch
            }
            _state.update { it.copy(busyRuleIds = it.busyRuleIds + ruleId) }
            try {
                val updated = api.updateRule(zone.zoneId, ruleId, row.rule.toUpdateRequest(enabled = enabled))
                // Some responses omit fields; keep what we sent when that happens.
                val merged = if (updated.ruleId.isBlank()) row.rule.copy(enabled = enabled) else updated
                replaceRow(ruleId, merged)
                _state.update { it.copy(message = "${row.display.title} ${if (enabled) "enabled" else "disabled"}.") }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(message = e.toUserMessage(permissionHint = "Email Routing Rules: Edit")) }
            } finally {
                _state.update { it.copy(busyRuleIds = it.busyRuleIds - ruleId) }
            }
        }
    }

    private fun replaceRow(ruleId: String, rule: EmailRoutingRule) {
        _state.update { current ->
            val data = current.dataState as? AliasListDataState.Loaded ?: return@update current
            current.copy(
                dataState = data.copy(aliases = data.aliases.map {
                    if (it.rule.ruleId == ruleId) AliasRow(rule, rule.toAliasDisplay()) else it
                })
            )
        }
    }
}
