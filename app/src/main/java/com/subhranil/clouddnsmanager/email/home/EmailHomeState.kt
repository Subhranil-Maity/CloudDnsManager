package com.subhranil.clouddnsmanager.email.home

import com.subhranil.clouddnsmanager.email.model.EmailRoutingSettings

sealed interface EmailHomeDataState {
    data object Loading : EmailHomeDataState
    data class Error(val message: String) : EmailHomeDataState
    data class Loaded(val settings: EmailRoutingSettings) : EmailHomeDataState
    /**
     * The token may not read the status (it needs "Zone Settings: Read", which aliases and
     * addresses don't). Not an error: the tabs still work, so only a quiet note is shown.
     */
    data object StatusNotPermitted : EmailHomeDataState
}

enum class EmailTab(val title: String) {
    Aliases("Aliases"),
    Addresses("Addresses"),
    Activity("Activity"),
}

data class EmailHomeState(
    val zoneName: String,
    /** Email Routing status for the zone (shown as a banner above the tabs). */
    val dataState: EmailHomeDataState = EmailHomeDataState.Loading,
    val selectedTab: EmailTab = EmailTab.Aliases,
)
