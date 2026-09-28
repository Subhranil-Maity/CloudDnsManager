package com.subhranil.clouddnsmanager.lock

sealed interface LockScreenIntent {
    data class UpdatePin(val pin: String) : LockScreenIntent
    data object Submit : LockScreenIntent
    data object BiometricSucceeded : LockScreenIntent
    data object ForgotPin : LockScreenIntent
    data object DismissForgotPin : LockScreenIntent
    data object ConfirmForgotPin : LockScreenIntent
}
