package com.subhranil.clouddnsmanager.nav

import android.util.Log
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationRouter {
    private val _navigationState = MutableStateFlow<List<NavKey>>(
        listOf(NavDestinations.StartScreenDestination)
    )
    val navigationState: StateFlow<List<NavKey>> = _navigationState.asStateFlow()

    // Completely clear the stack and set new destinations
    fun resetWithStack(newStack: List<NavKey>) {
        Log.d("NavRouter", "Stack Reset With ${newStack.toString()}")
        _navigationState.value = newStack
    }

    // Push a new screen onto the existing stack
    fun push(destination: NavKey) {
        _navigationState.value += destination
    }

    // Pop the top screen safely
    fun pop() {
        if (_navigationState.value.size > 1) {
            _navigationState.value = _navigationState.value.dropLast(1)
        }
    }
}