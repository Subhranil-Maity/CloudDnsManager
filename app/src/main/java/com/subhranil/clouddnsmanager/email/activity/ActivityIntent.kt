package com.subhranil.clouddnsmanager.email.activity

import com.subhranil.clouddnsmanager.email.model.EmailActivityEvent

sealed interface ActivityIntent {
    data object Retry : ActivityIntent
    data class Select(val event: EmailActivityEvent) : ActivityIntent
    data object DismissSelected : ActivityIntent
}
