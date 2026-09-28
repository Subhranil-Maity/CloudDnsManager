package com.subhranil.clouddnsmanager.security.settings

data class SecuritySettingsState(
    val isPinSet: Boolean = false,
    val biometricsEnabled: Boolean = false,
    val biometricsAvailable: Boolean = false,
    val showRemovePinConfirmation: Boolean = false,
    val message: String? = null,
)
