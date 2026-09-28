package com.subhranil.clouddnsmanager.email.aliases

sealed interface AliasListIntent {
    data object Retry : AliasListIntent
    data class Search(val query: String) : AliasListIntent
    /** An edit on Cloudflare: blocked while locked, then AuthGate, then PUT. */
    data class SetEnabled(val ruleId: String, val enabled: Boolean) : AliasListIntent
    data class Open(val ruleId: String) : AliasListIntent
    data object Create : AliasListIntent
    data object MessageShown : AliasListIntent
}
