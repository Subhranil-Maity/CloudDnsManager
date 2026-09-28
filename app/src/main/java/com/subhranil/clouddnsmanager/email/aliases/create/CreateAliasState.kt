package com.subhranil.clouddnsmanager.email.aliases.create

sealed interface DestinationOptionsState {
    data object Loading : DestinationOptionsState
    data class Error(val message: String) : DestinationOptionsState
    /** Only verified addresses can be used; [pendingCount] explains why others are missing. */
    data class Loaded(val verified: List<String>, val pendingCount: Int) : DestinationOptionsState
}

data class CreateAliasState(
    val zoneName: String,
    val localPart: String = "",
    val localPartError: String? = null,
    val name: String = "",
    val destinations: DestinationOptionsState = DestinationOptionsState.Loading,
    val selectedDestination: String? = null,
    val destinationError: String? = null,
    val saving: Boolean = false,
    /** Error from Cloudflare when saving. */
    val saveError: String? = null,
) {
    val fullAddress: String get() = "${localPart.trim().lowercase().ifEmpty { "…" }}@$zoneName"
}
