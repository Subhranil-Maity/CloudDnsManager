package com.subhranil.clouddnsmanager.email.addresses

import com.subhranil.clouddnsmanager.email.model.EmailDestinationAddress

sealed interface AddressListDataState {
    data object Loading : AddressListDataState
    data class Error(val message: String) : AddressListDataState
    data class Loaded(val addresses: List<EmailDestinationAddress>) : AddressListDataState
}

/** The "Add address" dialog. */
data class AddAddressForm(
    val email: String = "",
    val error: String? = null,
    val saving: Boolean = false,
)

data class AddressListState(
    val zoneName: String,
    val dataState: AddressListDataState = AddressListDataState.Loading,
    /**
     * Alias addresses in this zone that forward to each destination (lowercased email → aliases).
     * Null when the zone's rules couldn't be read, so delete warnings can say "unknown".
     */
    val aliasesByDestination: Map<String, List<String>>? = null,
    val lockedAddressIds: Set<String> = emptySet(),
    val notes: Map<String, String> = emptyMap(),
    /** Non-null while the Add dialog is open. */
    val addForm: AddAddressForm? = null,
    /** The address Cloudflare just emailed a verification link to (shows an info dialog). */
    val verificationSentTo: String? = null,
    /** Address whose details sheet is open. */
    val selectedId: String? = null,
    /** Address waiting for delete confirmation. */
    val confirmDeleteId: String? = null,
    val deletingId: String? = null,
    val message: String? = null,
) {
    val addresses: List<EmailDestinationAddress>
        get() = (dataState as? AddressListDataState.Loaded)?.addresses.orEmpty()

    fun address(id: String?): EmailDestinationAddress? = addresses.firstOrNull { it.addressId == id }

    fun aliasesUsing(email: String): List<String>? = aliasesByDestination?.let { it[email.lowercase()].orEmpty() }
}
