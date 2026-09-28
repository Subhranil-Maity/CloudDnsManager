package com.subhranil.clouddnsmanager.dns

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.dns.api.dnsApi
import com.subhranil.clouddnsmanager.dns.nav.DnsDestination
import com.subhranil.clouddnsmanager.dns.record.DNS_COMMENT_MAX_LENGTH
import com.subhranil.clouddnsmanager.dns.record.managedReason
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.localstore.ItemKey
import com.subhranil.clouddnsmanager.localstore.lock.ItemLockManager
import com.subhranil.clouddnsmanager.localstore.lock.LockMarker
import com.subhranil.clouddnsmanager.localstore.lock.LockResult
import com.subhranil.clouddnsmanager.localstore.lock.LockStatus
import com.subhranil.clouddnsmanager.localstore.lock.RemoteLockSync
import com.subhranil.clouddnsmanager.localstore.notes.NoteRepository
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.security.Authorizer
import com.subhranil.clouddnsmanager.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Records list + detail sheet for one zone.
 *
 * Every Cloudflare write (lock sync, delete) starts here, never in a composable
 * (architecture.md §8a). Records come from Cloudflare; locks and notes come from the shared
 * local store and are merged into [DnsRecordItem]s whenever any of the three changes.
 */
class DnsRecordViewModel(
    private val zoneId: String,
    private val router: NavigationRouter,
    private val sessionManager: SessionManager,
    private val authGate: Authorizer,
    private val lockManager: ItemLockManager,
    private val noteRepository: NoteRepository,
    private val changeNotifier: DnsRecordsChangeNotifier,
) : ViewModel() {

    private val _state = MutableStateFlow(DnsRecordState())
    val state = _state.asStateFlow()

    private val load = MutableStateFlow<DnsRecordsLoad>(DnsRecordsLoad.Loading)
    private val zonePrefix = ItemKey.dnsZonePrefix(zoneId)

    private var loadJob: Job? = null

    /** The running lock / unlock / delete; only one at a time so AuthGate prompts don't stack. */
    private var actionJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                load,
                lockManager.observeLockedItems(zonePrefix),
                noteRepository.observeNotes(zonePrefix),
            ) { load, lockedKeys, notes ->
                when (load) {
                    DnsRecordsLoad.Loading -> DnsRecordDataState.Loading
                    is DnsRecordsLoad.Failed -> DnsRecordDataState.Error(load.message)
                    is DnsRecordsLoad.Loaded -> DnsRecordDataState.DnsRecordData(
                        load.records.map { record ->
                            val key = keyOf(record)
                            DnsRecordItem(
                                record = record,
                                lockStatus = lockManager.status(key in lockedKeys, managedReason(record)),
                                note = notes[key],
                            )
                        }
                    )
                }
            }.collect { dataState -> _state.update { it.copy(dnsRecordDataState = dataState) } }
        }

        // The editor saves on its own screen; reload when it reports a change to this zone
        viewModelScope.launch {
            changeNotifier.changes.filter { it == zoneId }.collect { loadDnsRecords(silent = true) }
        }

        loadDnsRecords(silent = false)
    }

    fun onAction(intent: DnsRecordIntent) {
        when (intent) {
            is DnsRecordIntent.DismissDetailedDrawer -> dismissDetailedDrawer()
            is DnsRecordIntent.ShowDetailed -> showDrawer(intent.recordId)
            is DnsRecordIntent.Retry -> loadDnsRecords(silent = false)
            is DnsRecordIntent.Refresh -> loadDnsRecords(silent = true)
            is DnsRecordIntent.GoBack -> handleBackNavigation()
            is DnsRecordIntent.AddRecord -> router.push(DnsDestination.Editor(zoneId, zoneName(), recordId = null))
            is DnsRecordIntent.EditRecord -> editRecord(intent.recordId)
            is DnsRecordIntent.ToggleLock -> toggleLock(intent.recordId)
            is DnsRecordIntent.RequestDelete -> requestDelete(intent.recordId)
            is DnsRecordIntent.ConfirmDelete -> confirmDelete()
            is DnsRecordIntent.DismissDelete -> _state.update { it.copy(showDeleteConfirmation = false) }
            is DnsRecordIntent.UpdateNoteDraft -> _state.update { it.copy(noteDraft = intent.text) }
            is DnsRecordIntent.SaveNote -> saveNote()
            is DnsRecordIntent.ConsumeMessage -> _state.update { it.copy(message = null) }
        }
    }

    // ── Loading ────────────────────────────────────────────────────────────────

    /**
     * @param silent keep the current list on screen (after a change or a manual refresh)
     *   instead of going back to the shimmer, and report failures in the snackbar.
     */
    private fun loadDnsRecords(silent: Boolean) {
        // Cancel any in-flight load so rapid Retry taps don't run parallel collectors
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val showProgressively = !silent || load.value !is DnsRecordsLoad.Loaded
            if (showProgressively) load.value = DnsRecordsLoad.Loading
            _state.update { it.copy(refreshing = !showProgressively) }

            val client = sessionManager.clientOrNull()
            if (client == null) {
                fail("Session expired. Please log in again.", showProgressively)
                return@launch
            }

            try {
                // Cloudflare is the source of truth for synced locks; only write the ones that differ
                val lockedLocally = lockManager.observeLockedItems(zonePrefix).first()
                val accumulatedRecords = mutableListOf<DnsRecord>()

                client.allDnsRecords(zoneId).collect { record ->
                    accumulatedRecords.add(record)
                    val key = keyOf(record)
                    val lockedRemotely = LockMarker.isLocked(record.comment)
                    if (lockedRemotely != (key in lockedLocally)) {
                        lockManager.adoptRemoteState(key, lockedRemotely)
                    }
                    // First load: progressively emit snapshots so the list fills in page by page
                    if (showProgressively) load.value = DnsRecordsLoad.Loaded(accumulatedRecords.toList())
                }
                load.value = DnsRecordsLoad.Loaded(accumulatedRecords.toList())

                // Locks for records that no longer exist on Cloudflare
                val ids = accumulatedRecords.map { keyOf(it) }.toSet()
                (lockedLocally - ids).forEach { lockManager.forget(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DnsRecordViewModel", "Error fetching DNS records", e)
                fail(e.toUserMessage(permissionHint = "DNS: Read"), showProgressively)
            } finally {
                // A newer load that replaced this one owns the flag now
                if (isActive) _state.update { it.copy(refreshing = false) }
            }
        }
    }

    private fun fail(message: String, replaceList: Boolean) {
        if (replaceList) load.value = DnsRecordsLoad.Failed(message)
        else _state.update { it.copy(message = "Couldn't refresh: $message") }
    }

    // ── Detail sheet ───────────────────────────────────────────────────────────

    private fun showDrawer(recordId: String) {
        val item = findItem(recordId)
        _state.update {
            it.copy(
                openDetailedDrawer = recordId,
                noteDraft = item?.note.orEmpty(),
                showDeleteConfirmation = false,
                message = null,
            )
        }
    }

    private fun dismissDetailedDrawer() {
        // Messages shown inside the sheet go away with it
        _state.update { it.copy(openDetailedDrawer = null, showDeleteConfirmation = false, message = null) }
    }

    private fun editRecord(recordId: String) {
        val item = findItem(recordId) ?: return
        val blocked = blockedReason(item)
        if (blocked != null) {
            showMessage(blocked)
            return
        }
        dismissDetailedDrawer()
        router.push(DnsDestination.Editor(zoneId, zoneName(), recordId))
    }

    /** Private, device-only note: no Cloudflare write and no AuthGate (see NoteRepository). */
    private fun saveNote() {
        val item = _state.value.selectedItem ?: return
        val draft = _state.value.noteDraft
        viewModelScope.launch {
            noteRepository.setNote(keyOf(item.record), draft)
            showMessage(if (draft.isBlank()) "Note removed" else "Note saved on this device")
        }
    }

    // ── Locks ──────────────────────────────────────────────────────────────────

    /**
     * Locking needs no authentication; unlocking runs the AuthGate inside
     * [ItemLockManager.unlock], so it isn't asked for here as well. Either way the lock
     * marker is written to the record's Cloudflare comment first.
     */
    private fun toggleLock(recordId: String) {
        val item = findItem(recordId) ?: return
        if (actionJob?.isActive == true) return
        val status = item.lockStatus
        if (status is LockStatus.Managed) {
            showMessage("${status.reason}. It can't be locked or unlocked in this app.")
            return
        }
        val api = sessionManager.clientOrNull()?.dnsApi()
        if (api == null) {
            showMessage("Session expired. Please log in again.")
            return
        }

        val record = item.record
        val key = keyOf(record)
        val locking = status == LockStatus.Unlocked
        var updated: DnsRecord? = null
        val remote = RemoteLockSync { locked ->
            val comment = if (locked) {
                LockMarker.add(record.comment, maxLength = DNS_COMMENT_MAX_LENGTH)
            } else {
                LockMarker.remove(record.comment).ifEmpty { null }
            }
            updated = api.updateComment(zoneId, record.id, comment)
        }
        // The marker has to fit in Cloudflare's comment limit, which can cut a long comment
        val commentShortened = locking &&
            LockMarker.remove(LockMarker.add(record.comment, DNS_COMMENT_MAX_LENGTH)) != LockMarker.remove(record.comment)

        actionJob = viewModelScope.launch {
            _state.update { it.copy(working = true) }
            val result = if (locking) {
                lockManager.lock(key, remote)
            } else {
                lockManager.unlock(key, "Unlock DNS record ${record.name}", remote)
            }
            _state.update { it.copy(working = false) }

            when (result) {
                LockResult.Success -> {
                    updated?.let(::replaceRecord)
                    showMessage(
                        when {
                            !locking -> "Record unlocked"
                            commentShortened -> "Record locked. Its comment was shortened to fit the lock marker."
                            else -> "Record locked"
                        }
                    )
                    loadDnsRecords(silent = true)
                }
                LockResult.NotAuthorized -> showMessage("Authentication cancelled. The record is still locked.")
                is LockResult.Failed -> showMessage(
                    "Couldn't ${if (locking) "lock" else "unlock"} the record: " +
                        result.error.toUserMessage(permissionHint = "DNS: Edit")
                )
            }
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    private fun requestDelete(recordId: String) {
        val item = findItem(recordId) ?: return
        val blocked = blockedReason(item)
        if (blocked != null) {
            showMessage(blocked)
            return
        }
        _state.update { it.copy(openDetailedDrawer = recordId, showDeleteConfirmation = true) }
    }

    /** Confirmation dialog → AuthGate → DELETE → clean up local note and lock → reload. */
    private fun confirmDelete() {
        val item = _state.value.selectedItem ?: return
        _state.update { it.copy(showDeleteConfirmation = false) }
        if (actionJob?.isActive == true) return
        val record = item.record
        val key = keyOf(record)

        actionJob = viewModelScope.launch {
            // Re-check right before the write: the lock may have changed since the dialog opened
            val status = lockManager.status(lockManager.isLocked(key), managedReason(record))
            if (status != LockStatus.Unlocked) {
                showMessage(blockedReason(item.copy(lockStatus = status)) ?: "This record can't be deleted.")
                return@launch
            }
            if (!authGate.authorize("Delete DNS record ${record.name}")) return@launch

            val api = sessionManager.clientOrNull()?.dnsApi()
            if (api == null) {
                showMessage("Session expired. Please log in again.")
                return@launch
            }

            _state.update { it.copy(working = true) }
            try {
                api.deleteRecord(zoneId, record.id)
                noteRepository.deleteNote(key)
                lockManager.forget(key)
                load.update { current ->
                    if (current is DnsRecordsLoad.Loaded) current.copy(records = current.records.filter { it.id != record.id })
                    else current
                }
                _state.update { it.copy(openDetailedDrawer = null) }
                showMessage("Deleted ${record.type.name} record ${record.name}")
                loadDnsRecords(silent = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DnsRecordViewModel", "Error deleting DNS record", e)
                showMessage("Couldn't delete the record: " + e.toUserMessage(permissionHint = "DNS: Edit"))
            } finally {
                _state.update { it.copy(working = false) }
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Why [item] can't be edited or deleted right now, or null if it can. */
    private fun blockedReason(item: DnsRecordItem): String? = when (val status = item.lockStatus) {
        is LockStatus.Managed -> "${status.reason}. Cloudflare doesn't allow changing it here."
        LockStatus.UserLocked -> "This record is locked. Unlock it first."
        LockStatus.Unlocked -> null
    }

    private fun replaceRecord(updated: DnsRecord) {
        load.update { current ->
            if (current is DnsRecordsLoad.Loaded) {
                current.copy(records = current.records.map { if (it.id == updated.id) updated else it })
            } else current
        }
    }

    private fun findItem(recordId: String): DnsRecordItem? =
        (_state.value.dnsRecordDataState as? DnsRecordDataState.DnsRecordData)
            ?.dnsList?.find { it.record.id == recordId }

    /** Zone name from the loaded records (the list destination only carries the ID). */
    private fun zoneName(): String? =
        (load.value as? DnsRecordsLoad.Loaded)?.records?.firstNotNullOfOrNull { it.zoneName }

    private fun keyOf(record: DnsRecord) = ItemKey.dnsRecord(zoneId, record.id)

    private fun showMessage(message: String) {
        _state.update { it.copy(message = message) }
    }

    private fun handleBackNavigation() {
        router.pop()
    }
}
