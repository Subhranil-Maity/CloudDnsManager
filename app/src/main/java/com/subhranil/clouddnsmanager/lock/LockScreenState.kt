package com.subhranil.clouddnsmanager.lock

data class LockScreenState(
    val pin: String = "",
    val error: String? = null,
    val lockedOutUntilMs: Long = 0,
    val verifying: Boolean = false,
    val biometricsEnabled: Boolean = false,
    val showForgotPinConfirmation: Boolean = false,
)
