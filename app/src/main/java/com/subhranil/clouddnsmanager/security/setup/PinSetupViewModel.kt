package com.subhranil.clouddnsmanager.security.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.security.AppLockRepository
import com.subhranil.clouddnsmanager.security.PinHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinSetupStep { Enter, Confirm }

data class PinSetupState(
    val changing: Boolean,
    val step: PinSetupStep = PinSetupStep.Enter,
    val pin: String = "",
    val error: String? = null,
    val saving: Boolean = false,
)

sealed interface PinSetupIntent {
    data class UpdatePin(val pin: String) : PinSetupIntent
    data object Back : PinSetupIntent
}

/**
 * Full-screen "choose PIN → confirm PIN" flow. Only reachable from Security settings:
 * directly when no PIN exists yet, or after AuthGate approval when changing an existing PIN.
 */
class PinSetupViewModel(
    changing: Boolean,
    private val router: NavigationRouter,
    private val repository: AppLockRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PinSetupState(changing = changing))
    val state = _state.asStateFlow()

    /** The PIN from step 1, kept out of the UI state. */
    private var firstPin: String? = null

    fun onAction(intent: PinSetupIntent) {
        when (intent) {
            is PinSetupIntent.UpdatePin -> updatePin(intent.pin)
            PinSetupIntent.Back -> back()
        }
    }

    private fun updatePin(pin: String) {
        if (_state.value.saving) return
        _state.update { it.copy(pin = pin, error = null) }
        if (pin.length < PinHasher.PIN_LENGTH) return

        val chosen = firstPin
        when {
            chosen == null -> {
                firstPin = pin
                _state.update { it.copy(step = PinSetupStep.Confirm, pin = "") }
            }
            chosen == pin -> save(pin)
            else -> {
                firstPin = null
                _state.update {
                    it.copy(step = PinSetupStep.Enter, pin = "", error = "PINs didn't match. Choose your PIN again.")
                }
            }
        }
    }

    private fun save(pin: String) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.setPin(pin)
            firstPin = null
            router.pop()
        }
    }

    /** Back from the confirm step returns to step 1; from step 1 it leaves the screen. */
    private fun back() {
        if (_state.value.saving) return
        if (_state.value.step == PinSetupStep.Confirm) {
            firstPin = null
            _state.update { it.copy(step = PinSetupStep.Enter, pin = "", error = null) }
        } else {
            router.pop()
        }
    }
}
