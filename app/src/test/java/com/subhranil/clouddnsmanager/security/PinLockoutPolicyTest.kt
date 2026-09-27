package com.subhranil.clouddnsmanager.security

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PinLockoutPolicyTest {

    private val now = 1_000_000L

    private fun attempt(state: PinAttemptState, correct: Boolean, at: Long = now) =
        runBlocking { PinLockoutPolicy.evaluate(state, at) { correct } }

    @Test
    fun `first four wrong pins only count down`() {
        var state = PinAttemptState()
        for (expectedLeft in 4 downTo 1) {
            val (result, next) = attempt(state, correct = false)
            assertEquals(PinResult.Wrong(expectedLeft), result)
            state = next
        }
        assertEquals(4, state.failedAttempts)
    }

    @Test
    fun `fifth wrong pin locks out for 30 seconds`() {
        val (result, next) = attempt(PinAttemptState(failedAttempts = 4), correct = false)
        assertEquals(PinResult.LockedOut(now + 30_000), result)
        assertEquals(PinAttemptState(5, now + 30_000), next)
    }

    @Test
    fun `each further failure doubles the cooldown`() {
        val afterCooldown = now + 30_000
        val (result, next) = attempt(PinAttemptState(5, now + 30_000), correct = false, at = afterCooldown)
        assertEquals(PinResult.LockedOut(afterCooldown + 60_000), result)
        assertEquals(6, next.failedAttempts)
        assertEquals(120_000, PinLockoutPolicy.lockoutDurationMs(7))
    }

    @Test
    fun `pin is not checked at all during a cooldown`() {
        var checked = false
        val state = PinAttemptState(5, now + 30_000)
        val (result, next) = runBlocking {
            PinLockoutPolicy.evaluate(state, now + 1_000) { checked = true; true }
        }
        assertFalse(checked)
        assertEquals(PinResult.LockedOut(now + 30_000), result)
        assertEquals(state, next)
    }

    @Test
    fun `correct pin resets the counters`() {
        val (result, next) = attempt(PinAttemptState(failedAttempts = 3), correct = true)
        assertEquals(PinResult.Success, result)
        assertEquals(PinAttemptState(), next)
    }
}
