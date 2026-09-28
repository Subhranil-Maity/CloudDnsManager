package com.subhranil.clouddnsmanager.email.addresses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.email.EmailDataEvents
import com.subhranil.clouddnsmanager.email.EmailZone
import com.subhranil.clouddnsmanager.email.api.emailRoutingApi
import com.subhranil.clouddnsmanager.email.domain.AliasTarget
import com.subhranil.clouddnsmanager.email.domain.EmailValidators
import com.subhranil.clouddnsmanager.email.domain.toAliasDisplay
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.localstore.ItemKey
import com.subhranil.clouddnsmanager.localstore.lock.ItemLockManager
import com.subhranil.clouddnsmanager.localstore.lock.LockResult
import com.subhranil.clouddnsmanager.localstore.notes.NoteRepository
import com.subhranil.clouddnsmanager.security.AuthGate
import com.subhranil.clouddnsmanager.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ADDRESS_PERMISSION_READ = "Email Routing Addresses: Read"
private const val ADDRESS_PERMISSION_EDIT = "Email Routing Addresses: Edit"

/** Destination addresses (account-level): list, add, delete, plus local notes and locks. */
class AddressListViewModel(
    private val zone: EmailZone,
    private val sessionManager: SessionManager,
    private val authGate: AuthGate,
    private val lockManager: ItemLockManager,
    private val noteRepository: NoteRepository,
    private val events: EmailDataEvents,
) : ViewModel() {

    private val _state = MutableStateFlow(AddressListState(zoneName = zone.zoneName))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private val keyPrefix = ItemKey.emailAddressPrefix(zone.accountId)

    init {
        load()
        viewModelScope.launch {
            lockManager.observeLockedItems(keyPrefix).collect { keys ->
                _state.update { it.copy(lockedAddressIds = keys.map { key -> key.value.removePrefix(keyPrefix) }.toSet()) }
            }
        }
        viewModelScope.launch {
            noteRepository.observeNotes(keyPrefix).collect { notes ->
                _state.update { it.copy(notes = notes.mapKeys { (key, _) -> key.value.removePrefix(keyPrefix) }) }
            }
        }
        viewModelScope.launch { events.addressesChanged.collect { load(showSpinner = false) } }
        viewModelScope.launch { events.aliasesChanged.collect { load(showSpinner = false) } }
    }

    fun onAction(intent: AddressListIntent) {
        when (intent) {
            AddressListIntent.Retry -> load()
            AddressListIntent.ShowAdd -> _state.update { it.copy(addForm = AddAddressForm()) }
            AddressListIntent.DismissAdd -> _state.update { if (it.addForm?.saving == true) it else it.copy(addForm = null) }
            is AddressListIntent.UpdateAddEmail -> _state.update { s ->
                s.copy(addForm = s.addForm?.copy(email = intent.email, error = null))
            }
            AddressListIntent.SubmitAdd -> submitAdd()
            AddressListIntent.DismissVerificationInfo -> _state.update { it.copy(verificationSentTo = null) }
            is AddressListIntent.Select -> _state.update { it.copy(selectedId = intent.addressId) }
            AddressListIntent.DismissSelected -> _state.update { it.copy(selectedId = null) }
            is AddressListIntent.RequestDelete -> requestDelete(intent.addressId)
            AddressListIntent.ConfirmDelete -> confirmDelete()
            AddressListIntent.DismissDelete -> _state.update { it.copy(confirmDeleteId = null) }
            is AddressListIntent.SetLocked -> setLocked(intent.addressId, intent.locked)
            is AddressListIntent.SaveNote -> viewModelScope.launch {
                noteRepository.setNote(ItemKey.emailAddress(zone.accountId, intent.addressId), intent.note)
                _state.update { it.copy(message = if (intent.note.isBlank()) "Note removed." else "Note saved on this device.") }
            }
            AddressListIntent.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun load(showSpinner: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showSpinner) _state.update { it.copy(dataState = AddressListDataState.Loading) }
            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(dataState = AddressListDataState.Error("Session expired. Please log in again.")) }
                return@launch
            }
            // Rules are only used for "which aliases forward here"; their failure mustn't hide the list.
            val usage = async {
                runCatching {
                    val map = mutableMapOf<String, MutableList<String>>()
                    api.listRules(zone.zoneId).map { it.toAliasDisplay() }.forEach { alias ->
                        val target = alias.target as? AliasTarget.Forward ?: return@forEach
                        target.destinations.forEach { dest ->
                            map.getOrPut(dest.lowercase()) { mutableListOf() } += alias.title
                        }
                    }
                    map as Map<String, List<String>>
                }.getOrNull()
            }
            try {
                val addresses = api.listAddresses(zone.accountId).sortedBy { it.email.lowercase() }
                _state.update {
                    it.copy(dataState = AddressListDataState.Loaded(addresses), aliasesByDestination = usage.await())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update {
                    it.copy(dataState = AddressListDataState.Error(e.toUserMessage(permissionHint = ADDRESS_PERMISSION_READ)))
                }
            }
        }
    }

    private fun submitAdd() {
        val form = _state.value.addForm ?: return
        if (form.saving) return
        val result = EmailValidators.validateEmail(form.email)
        if (!result.isValid) {
            _state.update { it.copy(addForm = form.copy(error = result.error)) }
            return
        }
        if (_state.value.addresses.any { it.email.equals(result.value, ignoreCase = true) }) {
            _state.update { it.copy(addForm = form.copy(error = "This address is already in your list.")) }
            return
        }
        viewModelScope.launch {
            if (!authGate.authorize("Add destination address ${result.value}")) return@launch
            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(addForm = form.copy(error = "Session expired. Please log in again.")) }
                return@launch
            }
            _state.update { it.copy(addForm = form.copy(saving = true, error = null)) }
            try {
                val created = api.createAddress(zone.accountId, result.value)
                _state.update { s ->
                    val current = s.addresses
                    val added = if (created.addressId.isBlank()) current else current + created
                    s.copy(
                        addForm = null,
                        verificationSentTo = result.value,
                        dataState = AddressListDataState.Loaded(added.sortedBy { it.email.lowercase() }),
                    )
                }
                events.notifyAddressesChanged()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update {
                    it.copy(addForm = form.copy(saving = false, error = e.toUserMessage(permissionHint = ADDRESS_PERMISSION_EDIT)))
                }
            }
        }
    }

    private fun requestDelete(addressId: String) {
        viewModelScope.launch {
            if (lockManager.isLocked(ItemKey.emailAddress(zone.accountId, addressId))) {
                _state.update { it.copy(message = "This address is locked. Unlock it first.") }
            } else {
                _state.update { it.copy(confirmDeleteId = addressId) }
            }
        }
    }

    private fun confirmDelete() {
        val addressId = _state.value.confirmDeleteId ?: return
        val address = _state.value.address(addressId) ?: return
        _state.update { it.copy(confirmDeleteId = null) }
        viewModelScope.launch {
            val key = ItemKey.emailAddress(zone.accountId, addressId)
            // Checked again: the lock could have changed while the dialog was open
            if (lockManager.isLocked(key)) {
                _state.update { it.copy(message = "This address is locked. Unlock it first.") }
                return@launch
            }
            if (!authGate.authorize("Delete destination address ${address.email}")) return@launch
            val api = sessionManager.clientOrNull()?.emailRoutingApi()
            if (api == null) {
                _state.update { it.copy(message = "Session expired. Please log in again.") }
                return@launch
            }
            _state.update { it.copy(deletingId = addressId) }
            try {
                api.deleteAddress(zone.accountId, addressId)
                noteRepository.deleteNote(key)
                lockManager.forget(key)
                _state.update { s ->
                    s.copy(
                        dataState = AddressListDataState.Loaded(s.addresses.filterNot { it.addressId == addressId }),
                        selectedId = if (s.selectedId == addressId) null else s.selectedId,
                        message = "${address.email} deleted.",
                    )
                }
                events.notifyAddressesChanged()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(message = e.toUserMessage(permissionHint = ADDRESS_PERMISSION_EDIT)) }
            } finally {
                _state.update { it.copy(deletingId = null) }
            }
        }
    }

    private fun setLocked(addressId: String, locked: Boolean) {
        val key = ItemKey.emailAddress(zone.accountId, addressId)
        viewModelScope.launch {
            // lock() needs no auth by design; unlock() runs the AuthGate itself.
            val result = if (locked) lockManager.lock(key) else lockManager.unlock(key, "Unlock destination address")
            when (result) {
                LockResult.Success -> Unit
                LockResult.NotAuthorized -> _state.update { it.copy(message = "Still locked: authentication was cancelled.") }
                is LockResult.Failed -> _state.update { it.copy(message = result.error.toUserMessage()) }
            }
        }
    }
}
