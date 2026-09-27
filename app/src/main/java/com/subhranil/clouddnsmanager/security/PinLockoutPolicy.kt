package com.subhranil.clouddnsmanager.security

import kotlin.math.min

sealed interface PinResult {
    data object Success : PinResult
    data class Wrong(val attemptsBeforeLockout: Int) : PinResult
    data class LockedOut(val untilMs: Long) : PinResult
}

/** Persisted brute-force counters for the PIN. */
data class PinAttemptState(
    val failedAttempts: Int = 0,
    val lockoutUntilMs: Long = 0,
)

/**
 * Pure brute-force policy, kept free of storage/clock dependencies so it can be unit tested.
 *
 * The first wrong PINs are free; the [FREE_ATTEMPTS]th triggers a 30 s cooldown and every
 * further failure doubles it. Wrong PINs never wipe data.
 */
object PinLockoutPolicy {
    const val FREE_ATTEMPTS = 5
    const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_DOUBLINGS = 10 // caps the cooldown at ~8.5 hours

    /** Cooldown to apply after [failedAttempts] consecutive failures (0 = none). */
    fun lockoutDurationMs(failedAttempts: Int): Long =
        if (failedAttempts < FREE_ATTEMPTS) 0
        else BASE_LOCKOUT_MS shl min(failedAttempts - FREE_ATTEMPTS, MAX_DOUBLINGS)

    /**
     * Evaluate one PIN attempt. [pinMatches] is only invoked when no cooldown is active,
     * so a locked-out attacker gets no signal at all.
     */
    suspend fun evaluate(
        state: PinAttemptState,
        nowMs: Long,
        pinMatches: suspend () -> Boolean,
    ): Pair<PinResult, PinAttemptState> {
        if (nowMs < state.lockoutUntilMs) {
            return PinResult.LockedOut(state.lockoutUntilMs) to state
        }
        if (pinMatches()) {
            return PinResult.Success to PinAttemptState()
        }
        val failed = state.failedAttempts + 1
        val lockout = lockoutDurationMs(failed)
        return if (lockout > 0) {
            val until = nowMs + lockout
            PinResult.LockedOut(until) to PinAttemptState(failed, until)
        } else {
            PinResult.Wrong(FREE_ATTEMPTS - failed) to PinAttemptState(failed, 0)
        }
    }
}
