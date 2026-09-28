package com.subhranil.clouddnsmanager.security.setup

sealed interface PinSetupIntent {
    data class UpdatePin(val pin: String) : PinSetupIntent
    data object Back : PinSetupIntent
}
