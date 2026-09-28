package com.subhranil.clouddnsmanager.email.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.email.api.EmailActivityException
import com.subhranil.clouddnsmanager.email.api.emailRoutingApi
import com.subhranil.clouddnsmanager.email.api.toActivityMessage
import com.subhranil.clouddnsmanager.http.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * Incoming mail log from the GraphQL Analytics API (read-only).
 * [aliasFilter] blank = whole zone; otherwise only mail sent to that address.
 */
class ActivityViewModel(
    private val zoneId: String,
    aliasFilter: String,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val alias: String? = aliasFilter.trim().ifBlank { null }

    private val _state = MutableStateFlow(ActivityState(alias = alias))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(intent: ActivityIntent) {
        when (intent) {
            ActivityIntent.Retry -> load()
            is ActivityIntent.Select -> _state.update { it.copy(selected = intent.event) }
            ActivityIntent.DismissSelected -> _state.update { it.copy(selected = null) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(dataState = ActivityDataState.Loading) }
            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(dataState = ActivityDataState.Error("Session expired. Please log in again.")) }
                return@launch
            }
            // Retention differs by plan; if Cloudflare refuses the longer window, retry with a short one.
            var lastError: Exception? = null
            for (days in listOf(DEFAULT_WINDOW_DAYS, FALLBACK_WINDOW_DAYS)) {
                try {
                    val now = Instant.now()
                    val events = api.getActivity(
                        zoneId = zoneId,
                        since = now.minus(Duration.ofDays(days.toLong())),
                        until = now,
                        to = alias,
                    )
                    _state.update { it.copy(dataState = ActivityDataState.Loaded(events), windowDays = days) }
                    return@launch
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    lastError = e
                    // Only a GraphQL-level refusal (not permissions, not network) is worth retrying
                    if (e !is EmailActivityException || e.isPermissionError) break
                }
            }
            _state.update {
                it.copy(dataState = ActivityDataState.Error(lastError?.toActivityMessage() ?: "Couldn't load email activity."))
            }
        }
    }

    companion object {
        const val DEFAULT_WINDOW_DAYS = 7
        const val FALLBACK_WINDOW_DAYS = 1
    }
}
