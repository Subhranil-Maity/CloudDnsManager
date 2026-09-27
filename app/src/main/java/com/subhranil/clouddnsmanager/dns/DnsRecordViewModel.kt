package com.subhranil.clouddnsmanager.dns

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DnsRecordViewModel(
    private val zoneId: String,
    private val router: NavigationRouter,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(DnsRecordState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadDnsRecords()
    }

    private fun loadDnsRecords() {
        // Cancel any in-flight load so rapid Retry taps don't run parallel collectors
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Put the data block explicitly back into a pure Loading phase on initialization or retry clicks
            _state.update { it.copy(dnsRecordDataState = DnsRecordDataState.Loading) }

            val client = sessionManager.clientOrNull()
            if (client == null) {
                _state.update {
                    it.copy(dnsRecordDataState = DnsRecordDataState.Error("Session expired. Please log in again."))
                }
                return@launch
            }

            val accumulatedRecords = mutableListOf<DnsRecord>()

            // Clean, infinite stream handling built straight off of the Ktor Paginated Flow implementation
            client.allDnsRecords(zoneId)
                .catch { exception ->
                    Log.e("DnsRecordViewModel", "Error fetching DNS records", exception)
                    _state.update {
                        it.copy(
                            dnsRecordDataState = DnsRecordDataState.Error(
                                exception.message ?: "Failed to retrieve records from Cloudflare APIs."
                            )
                        )
                    }
                }
                .collect { singleRecord ->
                    accumulatedRecords.add(singleRecord)

                    // Progressively emit updating snapshots down into the Compose view framework
                    _state.update {
                        it.copy(dnsRecordDataState = DnsRecordDataState.DnsRecordData(accumulatedRecords.toList()))
                    }
                }
        }
    }

    fun onAction(intent: DnsRecordIntent) {
        when (intent) {
            is DnsRecordIntent.DismissDetailedDrawer -> dismissDetailedDrawer()
            is DnsRecordIntent.ShowDetailed -> showDrawer(intent.record)
            is DnsRecordIntent.Retry -> loadDnsRecords()
            is DnsRecordIntent.GoBack -> handleBackNavigation()
        }
    }

    private fun showDrawer(record: DnsRecord) {
        _state.update { it.copy(openDetailedDrawer = record) }
    }

    private fun dismissDetailedDrawer() {
        _state.update { it.copy(openDetailedDrawer = null) }
    }

    private fun handleBackNavigation() {
        router.pop()
    }
}