package com.subhranil.clouddnsmanager.http

import com.subhranil.clouddnsmanager.storage.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data object Loading : SessionState
    data class Authenticated(val client: CloudflareClient) : SessionState
}

class SessionManager(private val tokenStorage: TokenStorage) {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    /**
     * Run this exactly once inside your main entry point (Application class or MainActivity onCreate)
     * to safely decide whether to show the dashboard or kick the user to the setup flow.
     */
    suspend fun initialize() {
        val token = tokenStorage.getToken()
        if (token.isNullOrBlank()) {
            _sessionState.value = SessionState.Unauthenticated
        } else {
            // Token retrieved successfully from encrypted storage
            val client = CloudflareClient(token = token)
            _sessionState.value = SessionState.Authenticated(client)
        }
    }

    /**
     * Call this when a user submits their API Token on your setup/onboarding screen.
     */
    suspend fun login(token: String): Boolean {
        _sessionState.value = SessionState.Loading
        return try {
            val temporaryClient = CloudflareClient(token = token)

            // Perform a lightweight network check to confirm the token is working
            temporaryClient.verifyToken()

            // If the call succeeds, commit it to disk and transition state
            tokenStorage.saveToken(token)
            _sessionState.value = SessionState.Authenticated(temporaryClient)
            // FORCE YIELD: Guarantees StateFlow consumers process the Authenticated state
            // before the true boolean returns to the ViewModel
            kotlinx.coroutines.yield()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _sessionState.value = SessionState.Unauthenticated
            false // Verification failed (bad token or offline network)
        }
    }

    /**
     * Clear all persistent credentials out of the sandbox and dispose of engine threads.
     */
    suspend fun logout() {
        val currentState = _sessionState.value
        if (currentState is SessionState.Authenticated) {
            currentState.client.close() // Closes HTTP client engines cleanly
        }
        tokenStorage.clearToken()
        _sessionState.value = SessionState.Unauthenticated
    }
}