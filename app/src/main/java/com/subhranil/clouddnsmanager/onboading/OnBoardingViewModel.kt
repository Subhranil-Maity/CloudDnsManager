package com.subhranil.clouddnsmanager.onboading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.nav.NavDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnBoardingViewModel(
    private val sessionManager: SessionManager,
    private val router: NavigationRouter
) : ViewModel() {

    private val _state = MutableStateFlow<OnBoardingState>(OnBoardingState.Idle())
    val state = _state.asStateFlow()

    fun onAction(intent: OnBoardingIntent) {
        when (intent) {
            is OnBoardingIntent.UpdateToken -> updateToken(intent.token)
            is OnBoardingIntent.VerifyToken -> verifyToken()
            is OnBoardingIntent.DismissError -> dismissError()
            is OnBoardingIntent.Continue -> handleContinue()
        }
    }

    private fun updateToken(token: String) {
        when (val currentState = _state.value) {
            is OnBoardingState.Idle -> _state.value = OnBoardingState.Idle(token)
            is OnBoardingState.Error -> _state.value = OnBoardingState.Idle(token)
            else -> { /* Lock UI editing while active network validation occurs */ }
        }
    }

    private fun verifyToken() {
        val currentToken = when (val s = _state.value) {
            is OnBoardingState.Idle -> s.currentToken
            is OnBoardingState.Error -> s.currentToken
            else -> return // Block rapid duplicate taps
        }

        if (currentToken.isBlank()) {
            _state.value = OnBoardingState.Error(currentToken, "Token cannot be empty")
            return
        }

        viewModelScope.launch {
            _state.value = OnBoardingState.Verifying(currentToken)

            val isSuccess = sessionManager.login(currentToken)

            if (isSuccess) {
                _state.value = OnBoardingState.Verified
            } else {
                _state.value = OnBoardingState.Error(
                    currentToken = currentToken,
                    msg = "Invalid token or network failure. Please try again."
                )
            }
        }
    }

    private fun dismissError() {
        val currentToken = _state.value.token // Extracts token safely through our extension property
        _state.value = OnBoardingState.Idle(currentToken)
    }

    private fun handleContinue() {
        if (_state.value is OnBoardingState.Verified) {
            router.push(NavDestinations.SelectZonesDestination)
        }
    }
}