package com.subhranil.clouddnsmanager.dns.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.dns.DnsRecordsChangeNotifier
import com.subhranil.clouddnsmanager.dns.api.dnsApi
import com.subhranil.clouddnsmanager.dns.nav.DnsDestination
import com.subhranil.clouddnsmanager.dns.record.isEditableInApp
import com.subhranil.clouddnsmanager.dns.record.managedReason
import com.subhranil.clouddnsmanager.dns.record.supportsProxy
import com.subhranil.clouddnsmanager.dns.validation.DnsRecordValidator
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.localstore.ItemKey
import com.subhranil.clouddnsmanager.localstore.lock.ItemLockManager
import com.subhranil.clouddnsmanager.localstore.lock.LockMarker
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.security.Authorizer
import com.subhranil.clouddnsmanager.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Create / edit form for one DNS record.
 *
 * Save = validate → AuthGate → POST or PATCH → tell the list to reload → pop back.
 * Records that are managed by Cloudflare, locked, or of a type the editor doesn't support
 * open read-only.
 */
class DnsRecordEditorViewModel(
    private val destination: DnsDestination.Editor,
    private val router: NavigationRouter,
    private val sessionManager: SessionManager,
    private val authGate: Authorizer,
    private val lockManager: ItemLockManager,
    private val changeNotifier: DnsRecordsChangeNotifier,
) : ViewModel() {

    private val zoneId = destination.zoneId
    private val recordId = destination.recordId

    private val _state = MutableStateFlow(
        DnsRecordEditorState(isNew = recordId == null, zoneName = destination.zoneName)
    )
    val state = _state.asStateFlow()

    /** The record's comment on Cloudflare when it was loaded, used to keep the lock marker. */
    private var originalComment: String? = null

    /** Guards against a second tap starting another AuthGate prompt while one is running. */
    private var saveJob: Job? = null

    init {
        if (recordId != null) loadRecord(recordId)
        if (destination.zoneName == null) loadZoneName()
    }

    fun onAction(intent: DnsRecordEditorIntent) {
        when (intent) {
            is DnsRecordEditorIntent.SetType -> setType(intent)
            is DnsRecordEditorIntent.UpdateField -> updateForm(intent.field) { it.withField(intent.field, intent.value) }
            is DnsRecordEditorIntent.SetTtl -> updateForm(DnsField.TTL) { it.copy(ttl = intent.ttl) }
            is DnsRecordEditorIntent.SetProxied -> updateForm(DnsField.TTL) { it.copy(proxied = intent.proxied) }
            is DnsRecordEditorIntent.Save -> save()
            is DnsRecordEditorIntent.RetryLoad -> recordId?.let(::loadRecord)
            is DnsRecordEditorIntent.Back -> router.pop()
        }
    }

    private fun setType(intent: DnsRecordEditorIntent.SetType) {
        if (!_state.value.isNew) return
        _state.update {
            it.copy(
                form = it.form.copy(type = intent.type, proxied = it.form.proxied && intent.type.supportsProxy()),
                errors = emptyMap(),
                saveError = null,
            )
        }
    }

    /** Applies [change] and clears the error shown under [field]. */
    private fun updateForm(field: DnsField, change: (DnsRecordForm) -> DnsRecordForm) {
        if (_state.value.readOnlyReason != null) return
        _state.update { it.copy(form = change(it.form), errors = it.errors - field) }
    }

    // ── Loading ────────────────────────────────────────────────────────────────

    private fun loadRecord(recordId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadState = DnsEditorLoadState.Loading) }
            val client = sessionManager.clientOrNull()
            if (client == null) {
                _state.update { it.copy(loadState = DnsEditorLoadState.Error("Session expired. Please log in again.")) }
                return@launch
            }
            try {
                // Fetch fresh so the lock / managed checks use Cloudflare's current state
                val record = client.getDnsRecord(zoneId, recordId)
                originalComment = record.comment
                _state.update {
                    it.copy(
                        loadState = DnsEditorLoadState.Ready,
                        form = DnsRecordForm.fromRecord(record),
                        zoneName = record.zoneName ?: it.zoneName,
                        readOnlyReason = readOnlyReason(record),
                        ttlOptions = (COMMON_TTLS + record.ttl).distinct().sorted(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DnsRecordEditorVM", "Error loading DNS record", e)
                _state.update {
                    it.copy(loadState = DnsEditorLoadState.Error(e.toUserMessage(permissionHint = "DNS: Read")))
                }
            }
        }
    }

    /** Best effort: without it names are sent as typed, which Cloudflare also accepts. */
    private fun loadZoneName() {
        viewModelScope.launch {
            val client = sessionManager.clientOrNull() ?: return@launch
            try {
                val name = client.getZone(zoneId).name
                _state.update { if (it.zoneName == null) it.copy(zoneName = name) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("DnsRecordEditorVM", "Couldn't look up the zone name", e)
            }
        }
    }

    private suspend fun readOnlyReason(record: DnsRecord): String? {
        val managed = managedReason(record)
        return when {
            managed != null -> "$managed. Cloudflare doesn't allow changing it here."
            LockMarker.isLocked(record.comment) || lockManager.isLocked(ItemKey.dnsRecord(zoneId, record.id)) ->
                "This record is locked. Unlock it from the record details before editing."
            !record.type.isEditableInApp() ->
                "${record.type.name} records can't be edited in this app yet. Use the Cloudflare dashboard to change them."
            else -> null
        }
    }

    // ── Saving ─────────────────────────────────────────────────────────────────

    private fun save() {
        val current = _state.value
        if (!current.canSave || saveJob?.isActive == true) return

        val errors = DnsRecordValidator.validate(current.form)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, saveError = "Fix the highlighted fields.") }
            return
        }

        saveJob = viewModelScope.launch {
            // An existing record may have been locked since the form opened
            if (recordId != null && lockManager.isLocked(ItemKey.dnsRecord(zoneId, recordId))) {
                _state.update { it.copy(saveError = "This record is locked. Unlock it before saving changes.") }
                return@launch
            }

            val reason = if (recordId == null) "Create DNS record" else "Save changes to DNS record"
            if (!authGate.authorize(reason)) return@launch

            val api = sessionManager.clientOrNull()?.dnsApi()
            if (api == null) {
                _state.update { it.copy(saveError = "Session expired. Please log in again.") }
                return@launch
            }

            _state.update { it.copy(saving = true, saveError = null, errors = emptyMap()) }
            try {
                val request = DnsRecordRequestBuilder.build(current.form, _state.value.zoneName, originalComment)
                if (recordId == null) api.createRecord(zoneId, request)
                else api.updateRecord(zoneId, recordId, request)

                changeNotifier.notifyChanged(zoneId)
                router.pop()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DnsRecordEditorVM", "Error saving DNS record", e)
                _state.update { it.copy(saving = false, saveError = e.toUserMessage(permissionHint = "DNS: Edit")) }
            }
        }
    }
}
