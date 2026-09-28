package com.subhranil.clouddnsmanager.email.addresses

sealed interface AddressListIntent {
    data object Retry : AddressListIntent

    data object ShowAdd : AddressListIntent
    data object DismissAdd : AddressListIntent
    data class UpdateAddEmail(val email: String) : AddressListIntent
    /** Validate, then AuthGate, then POST. */
    data object SubmitAdd : AddressListIntent
    data object DismissVerificationInfo : AddressListIntent

    data class Select(val addressId: String) : AddressListIntent
    data object DismissSelected : AddressListIntent

    data class RequestDelete(val addressId: String) : AddressListIntent
    /** After the confirmation dialog: AuthGate, then DELETE. */
    data object ConfirmDelete : AddressListIntent
    data object DismissDelete : AddressListIntent

    data class SetLocked(val addressId: String, val locked: Boolean) : AddressListIntent
    data class SaveNote(val addressId: String, val note: String) : AddressListIntent

    data object MessageShown : AddressListIntent
}
