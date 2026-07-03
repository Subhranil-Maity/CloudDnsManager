package com.subhranil.clouddnsmanager.onboading

sealed interface OnBoardingState {
    // Initial State: Screen is waiting for input
    data class Idle(val currentToken: String = "") : OnBoardingState

    // Loading State: Actively communicating with Cloudflare APIs
    data class Verifying(val currentToken: String) : OnBoardingState

    // Failure State: Something went wrong (bad token, no internet, etc.)
    data class Error(val currentToken: String, val msg: String) : OnBoardingState

    // Success State: Everything checked out!
    data object Verified : OnBoardingState
}

// --- Syntactic Sugar Extension Properties for Compose UI Cleanliness ---
val OnBoardingState.token: String
    get() = when (this) {
        is OnBoardingState.Idle -> currentToken
        is OnBoardingState.Verifying -> currentToken
        is OnBoardingState.Error -> currentToken
        OnBoardingState.Verified -> "" // Or preserve a historical token string if needed
    }

val OnBoardingState.isTokenVerifying: Boolean
    get() = this is OnBoardingState.Verifying

val OnBoardingState.isTokenVerified: Boolean
    get() = this is OnBoardingState.Verified

val OnBoardingState.error: String?
    get() = (this as? OnBoardingState.Error)?.msg