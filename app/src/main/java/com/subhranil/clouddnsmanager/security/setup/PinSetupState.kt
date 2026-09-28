package com.subhranil.clouddnsmanager.security.setup

enum class PinSetupStep { Enter, Confirm }

data class PinSetupState(
    val changing: Boolean,
    val step: PinSetupStep = PinSetupStep.Enter,
    val pin: String = "",
    val error: String? = null,
    val saving: Boolean = false,
)
