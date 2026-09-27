package com.subhranil.clouddnsmanager.security.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.security.AppLockRepository
import com.subhranil.clouddnsmanager.security.AuthGate
import com.subhranil.clouddnsmanager.security.BiometricAuthenticator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecuritySettingsState(
    val isPinSet: Boolean = false,
    val biometricsEnabled: Boolean = false,
    val biometricsAvailable: Boolean = false,
    val showRemovePinConfirmation: Boolean = false,
    val message: String? = null,
)

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

class SecuritySettingsViewModel(
    private val router: NavigationRouter,
    private val repository: AppLockRepository,
    private val authGate: AuthGate,
    biometrics: BiometricAuthenticator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SecuritySettingsState(biometricsAvailable = biometrics.canUseBiometrics())
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.isPinSet, repository.biometricsEnabled, ::Pair).collect { (pinSet, bio) ->
                _state.update { it.copy(isPinSet = pinSet, biometricsEnabled = bio) }
            }
        }
    }

    fun onAction(intent: SecuritySettingsIntent) {
        when (intent) {
            SecuritySettingsIntent.Back -> router.pop()
            // Creating the first PIN only adds protection, so it needs no prior auth
            SecuritySettingsIntent.SetPin -> router.push(NavDestinations.PinSetup(changing = false))
            SecuritySettingsIntent.ChangePin -> authorizeThen("Change your app PIN") {
                router.push(NavDestinations.PinSetup(changing = true))
            }
            SecuritySettingsIntent.RemovePin -> _state.update { it.copy(showRemovePinConfirmation = true) }
            SecuritySettingsIntent.DismissRemovePin -> _state.update { it.copy(showRemovePinConfirmation = false) }
            SecuritySettingsIntent.ConfirmRemovePin -> {
                _state.update { it.copy(showRemovePinConfirmation = false) }
                authorizeThen("Remove your app PIN") {
                    repository.clearPin()
                    _state.update { it.copy(message = "PIN removed") }
                }
            }
            SecuritySettingsIntent.EnableBiometricsVerified -> viewModelScope.launch {
                repository.setBiometricsEnabled(true)
            }
            SecuritySettingsIntent.DisableBiometrics -> authorizeThen("Turn off biometric unlock") {
                repository.setBiometricsEnabled(false)
            }
            SecuritySettingsIntent.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    /** Security rule: changes to the lock itself must pass the AuthGate first. */
    private fun authorizeThen(reason: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            if (authGate.authorize(reason)) action()
        }
    }
}
