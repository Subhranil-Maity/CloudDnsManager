package com.subhranil.clouddnsmanager.email.aliases.detail

sealed interface AliasDetailIntent {
    data object Retry : AliasDetailIntent
    data object Back : AliasDetailIntent

    /** Edit on Cloudflare: blocked while locked, then AuthGate, then PUT. */
    data class SetEnabled(val enabled: Boolean) : AliasDetailIntent

    data object StartEdit : AliasDetailIntent
    data object CancelEdit : AliasDetailIntent
    data class UpdateEditName(val name: String) : AliasDetailIntent
    data class SelectEditDestination(val email: String) : AliasDetailIntent
    /** AuthGate, then PUT. */
    data object SaveEdit : AliasDetailIntent

    data object RequestDelete : AliasDetailIntent
    /** After the confirmation dialog: AuthGate, then DELETE. */
    data object ConfirmDelete : AliasDetailIntent
    data object DismissDelete : AliasDetailIntent

    /** Lock needs no auth; unlock goes through ItemLockManager, which runs the AuthGate. */
    data class SetLocked(val locked: Boolean) : AliasDetailIntent
    data class SaveNote(val note: String) : AliasDetailIntent

    data object MessageShown : AliasDetailIntent
}
