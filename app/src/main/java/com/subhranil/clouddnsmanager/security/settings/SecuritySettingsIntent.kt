package com.subhranil.clouddnsmanager.security.settings

sealed interface SecuritySettingsIntent {
    data object Back : SecuritySettingsIntent
    data object SetPin : SecuritySettingsIntent
    data object ChangePin : SecuritySettingsIntent
    data object RemovePin : SecuritySettingsIntent
    data object ConfirmRemovePin : SecuritySettingsIntent
    data object DismissRemovePin : SecuritySettingsIntent
    /** Sent by the screen after a successful biometric prompt. */
    data object EnableBiometricsVerified : SecuritySettingsIntent
    data object DisableBiometrics : SecuritySettingsIntent
    data object MessageShown : SecuritySettingsIntent
}
