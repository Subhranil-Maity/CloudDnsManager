package com.subhranil.clouddnsmanager.email.activity

import com.subhranil.clouddnsmanager.email.model.EmailActivityEvent

sealed interface ActivityDataState {
    data object Loading : ActivityDataState
    data class Error(val message: String) : ActivityDataState
    data class Loaded(val events: List<EmailActivityEvent>) : ActivityDataState
}

data class ActivityState(
    /** Null for the whole zone, otherwise the one alias address being shown. */
    val alias: String?,
    val dataState: ActivityDataState = ActivityDataState.Loading,
    /** How far back the loaded list goes. Shrinks if Cloudflare refuses the longer window. */
    val windowDays: Int = ActivityViewModel.DEFAULT_WINDOW_DAYS,
    val selected: EmailActivityEvent? = null,
)
