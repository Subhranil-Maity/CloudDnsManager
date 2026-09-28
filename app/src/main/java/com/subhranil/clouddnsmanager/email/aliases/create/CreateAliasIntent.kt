package com.subhranil.clouddnsmanager.email.aliases.create

sealed interface CreateAliasIntent {
    data class UpdateLocalPart(val value: String) : CreateAliasIntent
    data object GenerateRandom : CreateAliasIntent
    data class UpdateName(val value: String) : CreateAliasIntent
    data class SelectDestination(val email: String) : CreateAliasIntent
    data object RetryDestinations : CreateAliasIntent
    data object OpenAddresses : CreateAliasIntent
    /** Validate, then AuthGate, then POST. */
    data object Save : CreateAliasIntent
    data object Back : CreateAliasIntent
}
