package com.subhranil.clouddnsmanager.zone

sealed interface ZoneHubIntent {
    data object OpenDns : ZoneHubIntent
    data object OpenEmail : ZoneHubIntent
    data object Back : ZoneHubIntent
}
