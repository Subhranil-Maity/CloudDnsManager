package com.subhranil.clouddnsmanager.email.aliases

import com.subhranil.clouddnsmanager.email.domain.AliasDisplay
import com.subhranil.clouddnsmanager.email.model.EmailRoutingRule

sealed interface AliasListDataState {
    data object Loading : AliasListDataState
    data class Error(val message: String) : AliasListDataState
    data class Loaded(
        /** Rules as Cloudflare sent them, paired with their display form. */
        val aliases: List<AliasRow>,
        /** Null when the zone has no catch-all or it couldn't be read. */
        val catchAll: AliasDisplay?,
    ) : AliasListDataState
}

data class AliasRow(val rule: EmailRoutingRule, val display: AliasDisplay)

data class AliasListState(
    val zoneName: String,
    val dataState: AliasListDataState = AliasListDataState.Loading,
    val query: String = "",
    /** Rule ids locked locally (ItemLockManager). */
    val lockedRuleIds: Set<String> = emptySet(),
    /** Rule ids with a local note. */
    val notedRuleIds: Set<String> = emptySet(),
    /** Rules with an enable/disable request in flight. */
    val busyRuleIds: Set<String> = emptySet(),
    /** One-off message shown as a snackbar. */
    val message: String? = null,
) {
    val visibleAliases: List<AliasRow>
        get() = (dataState as? AliasListDataState.Loaded)?.aliases.orEmpty()
            .filter { it.display.matchesQuery(query) }
}
