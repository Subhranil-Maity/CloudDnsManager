package com.subhranil.clouddnsmanager.onboading

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.storage.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnBoardingViewModel(
    private val dataStore: DataStore<UserPreferences>,
    private val router: NavigationRouter,
    ) : ViewModel() {
    private val _state = MutableStateFlow(OnBoardingState())
    val state = _state.asStateFlow()

    public fun onAction(
        intent: OnBoardingIntent
    ) {
        when (intent) {
            is OnBoardingIntent.UpdateToken -> updateToken(intent.token)
            is OnBoardingIntent.VerifyToken -> verifyToken()
            is OnBoardingIntent.DismissError -> _state.update {
                it.copy(error = null)
            }
            is OnBoardingIntent.Continue -> continueToNext()
        }
    }

    fun continueToNext() {
        if (!_state.value.isTokenVerified) return
        router.resetWithStack(listOf(NavDestinations.StartScreenDestination))
    }

    private fun updateToken(token: String) {
        _state.update {
            it.copy(token = token)
        }
    }

    private fun verifyToken() {
        viewModelScope.launch {
            _state.update {
                it.copy(isTokenVerifying = true)
            }
            val client = CloudflareClient(_state.value.token)
            try {
                val data = client.verifyToken()
                // TODO check permisions
                dataStore.updateData { it ->
                    it.copy(
                        token = _state.value.token
                    )
                }
                _state.update {
                    it.copy(isTokenVerified = true)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message)
                }
                e.printStackTrace()
            }
            _state.update {
                it.copy(isTokenVerifying = false)
            }
        }
    }
}
