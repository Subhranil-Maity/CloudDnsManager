package com.subhranil.clouddnsmanager.email.aliases.detail

import com.subhranil.clouddnsmanager.email.domain.AliasDisplay
import com.subhranil.clouddnsmanager.email.model.EmailRoutingRule

sealed interface AliasDetailDataState {
    data object Loading : AliasDetailDataState
    data class Error(val message: String) : AliasDetailDataState
    data class Loaded(val rule: EmailRoutingRule, val display: AliasDisplay) : AliasDetailDataState
}

/** The inline edit form (destination + rule name). */
data class AliasEditForm(
    val name: String,
    /** Null when the rule isn't a forward rule; only the name can be edited then. */
    val destination: String?,
    val saving: Boolean = false,
    val error: String? = null,
)

data class AliasDetailState(
    val zoneName: String,
    val dataState: AliasDetailDataState = AliasDetailDataState.Loading,
    val locked: Boolean = false,
    val note: String? = null,
    /** Verified destination addresses for the edit picker; null while loading or if unavailable. */
    val verifiedDestinations: List<String>? = null,
    val edit: AliasEditForm? = null,
    val togglingEnabled: Boolean = false,
    val confirmDelete: Boolean = false,
    val deleting: Boolean = false,
    val message: String? = null,
) {
    /** Any write in flight: the UI disables other actions meanwhile. */
    val busy: Boolean get() = togglingEnabled || deleting || edit?.saving == true
}
