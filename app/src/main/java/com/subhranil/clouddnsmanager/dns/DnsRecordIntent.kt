package com.subhranil.clouddnsmanager.dns

import kotlinx.serialization.Serializable

@Serializable
sealed interface DnsRecordIntent {
    @Serializable
    data object DismissDetailedDrawer : DnsRecordIntent

    @Serializable
    data class ShowDetailed(val recordId: String) : DnsRecordIntent

    /** Full reload after a failed load. */
    @Serializable
    data object Retry : DnsRecordIntent

    /** Background reload that keeps the current list on screen. */
    @Serializable
    data object Refresh : DnsRecordIntent

    @Serializable
    data object GoBack : DnsRecordIntent

    @Serializable
    data object AddRecord : DnsRecordIntent

    @Serializable
    data class EditRecord(val recordId: String) : DnsRecordIntent

    /** Lock an unlocked record, or unlock (after authentication) a user-locked one. */
    @Serializable
    data class ToggleLock(val recordId: String) : DnsRecordIntent

    @Serializable
    data class RequestDelete(val recordId: String) : DnsRecordIntent

    @Serializable
    data object ConfirmDelete : DnsRecordIntent

    @Serializable
    data object DismissDelete : DnsRecordIntent

    @Serializable
    data class UpdateNoteDraft(val text: String) : DnsRecordIntent

    @Serializable
    data object SaveNote : DnsRecordIntent

    @Serializable
    data object ConsumeMessage : DnsRecordIntent
}
