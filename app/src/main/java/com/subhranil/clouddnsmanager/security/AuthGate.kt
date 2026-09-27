package com.subhranil.clouddnsmanager.security

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Single entry point for the rule "every destructive action or change must pass
 * biometric / PIN authentication" (see architecture.md → Security Rules).
 *
 * ViewModels call [authorize] *after* their confirmation dialog and only proceed on `true`.
 * The prompt itself is rendered by [AuthGateHost], which lives once in MainActivity.
 */
class AuthGate {

    internal class Request(val reason: String) {
        val result = CompletableDeferred<Boolean>()
    }

    private val _requests = Channel<Request>(Channel.UNLIMITED)
    internal val requests: ReceiveChannel<Request> = _requests

    /** Suspends until the user passes (true) or cancels / fails (false) authentication. */
    suspend fun authorize(reason: String): Boolean {
        val request = Request(reason)
        _requests.send(request)
        return try {
            request.result.await()
        } finally {
            // Caller cancelled (e.g. ViewModel cleared) — make sure nothing waits forever
            request.result.complete(false)
        }
    }
}
