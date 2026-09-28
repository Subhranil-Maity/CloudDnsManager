package com.subhranil.clouddnsmanager.dns

import com.subhranil.clouddnsmanager.dns.record.displayComment
import com.subhranil.clouddnsmanager.dns.record.isEditableInApp
import com.subhranil.clouddnsmanager.localstore.lock.LockStatus
import com.subhranil.clouddnsmanager.models.dns.DnsRecord

/** A record plus what this device knows about it: its lock state and private note. */
data class DnsRecordItem(
    val record: DnsRecord,
    val lockStatus: LockStatus,
    val note: String?,
) {
    /** Cloudflare's comment without the lock marker. */
    val comment: String? get() = record.displayComment()
    val hasNote: Boolean get() = !note.isNullOrBlank()
    /** The editor supports this record type. */
    val typeEditable: Boolean get() = record.type.isEditableInApp()
}

sealed interface DnsRecordDataState {
    data object Loading : DnsRecordDataState
    data class Error(val error: String) : DnsRecordDataState
    data class DnsRecordData(val dnsList: List<DnsRecordItem>) : DnsRecordDataState
}

/** Raw result of fetching records from Cloudflare, before locks and notes are merged in. */
internal sealed interface DnsRecordsLoad {
    data object Loading : DnsRecordsLoad
    data class Failed(val message: String) : DnsRecordsLoad
    data class Loaded(val records: List<DnsRecord>) : DnsRecordsLoad
}

data class DnsRecordState(
    val dnsRecordDataState: DnsRecordDataState = DnsRecordDataState.Loading,
    /** ID of the record whose detail sheet is open. */
    val openDetailedDrawer: String? = null,
    /** Reloading in the background while the current list stays visible. */
    val refreshing: Boolean = false,
    /** A lock, unlock or delete is running for the open record. */
    val working: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    /** Private note being edited in the detail sheet. */
    val noteDraft: String = "",
    /** One-off message for the snackbar; cleared with [DnsRecordIntent.ConsumeMessage]. */
    val message: String? = null,
) {
    val selectedItem: DnsRecordItem?
        get() = (dnsRecordDataState as? DnsRecordDataState.DnsRecordData)
            ?.dnsList?.find { it.record.id == openDetailedDrawer }
}
