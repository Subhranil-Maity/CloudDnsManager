package com.subhranil.clouddnsmanager.start

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhranil.clouddnsmanager.nav.NavDestinations
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.storage.UserPreferences
import com.subhranil.clouddnsmanager.storage.UserPreferencesSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainState(
    val loading: Boolean = true
)

class StartViewModel(
    private val router: NavigationRouter,
    private val dataStore: DataStore<UserPreferences>
): ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow();


    init {
        viewModelScope.launch {
            Log.d("Start", "Retriving Token")
            val token = dataStore.data.first().token ?: "";
            Log.d("Start", "Token is: $token")
            if (token.isEmpty()) {
                router.resetWithStack(listOf(NavDestinations.OnBoarding))
            } else {
                router.resetWithStack(listOf(NavDestinations.SelectZonesDestination))
            }
        }
    }
}