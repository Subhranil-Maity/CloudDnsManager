package com.subhranil.clouddnsmanager.email.home

import com.subhranil.clouddnsmanager.email.model.EmailRoutingSettings

sealed interface EmailHomeDataState {
    data object Loading : EmailHomeDataState
    data class Error(val message: String) : EmailHomeDataState
    data class Loaded(val settings: EmailRoutingSettings) : EmailHomeDataState
}

data class EmailHomeState(
    val zoneName: String,
    val dataState: EmailHomeDataState = EmailHomeDataState.Loading,
)
