package com.subhranil.clouddnsmanager.dns.edit

sealed interface DnsEditorLoadState {
    /** Fetching the record being edited. */
    data object Loading : DnsEditorLoadState
    data class Error(val message: String) : DnsEditorLoadState
    data object Ready : DnsEditorLoadState
}

data class DnsRecordEditorState(
    val isNew: Boolean,
    val loadState: DnsEditorLoadState = if (isNew) DnsEditorLoadState.Ready else DnsEditorLoadState.Loading,
    val zoneName: String? = null,
    val form: DnsRecordForm = DnsRecordForm(),
    val errors: Map<DnsField, String> = emptyMap(),
    /** Non-null when the record can't be changed here; the form is shown read-only with this reason. */
    val readOnlyReason: String? = null,
    val ttlOptions: List<Int> = COMMON_TTLS,
    val saving: Boolean = false,
    /** Cloudflare's answer when saving failed, already turned into UI text. */
    val saveError: String? = null,
) {
    val canSave: Boolean
        get() = loadState == DnsEditorLoadState.Ready && readOnlyReason == null && !saving
}
