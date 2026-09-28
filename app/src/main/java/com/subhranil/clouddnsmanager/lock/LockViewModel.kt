package com.subhranil.clouddnsmanager.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.security.AppLockManager
import com.subhranil.clouddnsmanager.security.AppLockRepository
import com.subhranil.clouddnsmanager.security.PinHasher
import com.subhranil.clouddnsmanager.security.PinResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LockViewModel(
    private val repository: AppLockRepository,
    private val lockManager: AppLockManager,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LockScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(lockedOutUntilMs = repository.lockoutUntilMs()) }
        }
        viewModelScope.launch {
            repository.biometricsEnabled.collect { enabled ->
                _state.update { it.copy(biometricsEnabled = enabled) }
            }
        }
    }

    fun onAction(intent: LockScreenIntent) {
        when (intent) {
            is LockScreenIntent.UpdatePin -> {
                _state.update { it.copy(pin = intent.pin, error = null) }
                if (intent.pin.length == PinHasher.PIN_LENGTH) submit()
            }
            LockScreenIntent.Submit -> submit()
            LockScreenIntent.BiometricSucceeded -> viewModelScope.launch {
                repository.resetFailedAttempts()
                unlock()
            }
            LockScreenIntent.ForgotPin -> _state.update { it.copy(showForgotPinConfirmation = true) }
            LockScreenIntent.DismissForgotPin -> _state.update { it.copy(showForgotPinConfirmation = false) }
            LockScreenIntent.ConfirmForgotPin -> forgotPin()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.verifying || current.pin.length < PinHasher.PIN_LENGTH) return
        _state.update { it.copy(verifying = true) }
        viewModelScope.launch {
            when (val result = repository.verifyPin(current.pin)) {
                PinResult.Success -> unlock()
                is PinResult.Wrong -> _state.update {
                    it.copy(
                        pin = "",
                        verifying = false,
                        error = "Incorrect PIN. ${result.attemptsBeforeLockout} attempts left before a cooldown.",
                    )
                }
                is PinResult.LockedOut -> _state.update {
                    it.copy(pin = "", verifying = false, error = null, lockedOutUntilMs = result.untilMs)
                }
            }
        }
    }

    private fun unlock() {
        // This ViewModel outlives the lock screen, so reset it for the next lock
        _state.update { it.copy(pin = "", error = null, verifying = false, lockedOutUntilMs = 0) }
        lockManager.unlock()
    }

    /**
     * Recovery path: wipes the token and the PIN (logout clears all preferences).
     * Deliberately not behind AuthGate — erasing credentials exposes nothing.
     */
    private fun forgotPin() {
        _state.update { it.copy(showForgotPinConfirmation = false) }
        viewModelScope.launch {
            sessionManager.logout()
            unlock()
        }
    }
}
