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
        // Deliberately not switching to Loading here: that would make MainActivity tear down
        // the navigation tree (and the onboarding screen's error state) mid-verification.
        // The onboarding screen shows its own Verifying spinner instead.
        val temporaryClient = CloudflareClient(token = token)
        return try {
            // Perform a lightweight network check to confirm the token is working
            temporaryClient.verifyToken()

            // If the call succeeds, commit it to disk and transition state
            tokenStorage.saveToken(token)
            _sessionState.value = SessionState.Authenticated(temporaryClient)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            temporaryClient.close()
            false // Verification failed (bad token or offline network)
        }
    }

    /** The active client, or null when there is no authenticated session. */
    fun clientOrNull(): CloudflareClient? =
        (_sessionState.value as? SessionState.Authenticated)?.client

    /**
     * Clear all persistent credentials out of the sandbox and dispose of engine threads.
     */
    suspend fun logout() {
        val currentState = _sessionState.value
        if (currentState is SessionState.Authenticated) {
            currentState.client.close() // Closes HTTP client engines cleanly
        }
        tokenStorage.clearAll() // also removes the app PIN / biometric settings
        _sessionState.value = SessionState.Unauthenticated
    }
}