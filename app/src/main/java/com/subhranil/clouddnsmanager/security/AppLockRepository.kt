package com.subhranil.clouddnsmanager.security

import androidx.datastore.core.DataStore
import com.subhranil.clouddnsmanager.storage.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Base64

/** Stores and verifies the optional app PIN and the biometric-unlock preference. */
class AppLockRepository(
    private val dataStore: DataStore<UserPreferences>,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val isPinSet: Flow<Boolean> =
        dataStore.data.map { it.pinHash != null }.distinctUntilChanged()

    /** Biometrics only ever apply on top of a PIN (the PIN is the fallback). */
    val biometricsEnabled: Flow<Boolean> =
        dataStore.data.map { it.pinHash != null && it.biometricsEnabled }.distinctUntilChanged()

    suspend fun isPinSetNow(): Boolean = isPinSet.first()

    suspend fun biometricsEnabledNow(): Boolean = biometricsEnabled.first()

    suspend fun lockoutUntilMs(): Long = dataStore.data.first().lockoutUntilMs

    suspend fun setPin(pin: String) {
        require(PinHasher.isValidPin(pin)) { "PIN must be ${PinHasher.PIN_LENGTH} digits" }
        val salt = PinHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
        dataStore.updateData {
            it.copy(
                pinHash = hash.toBase64(),
                pinSalt = salt.toBase64(),
                failedPinAttempts = 0,
                lockoutUntilMs = 0,
            )
        }
    }

    suspend fun clearPin() {
        dataStore.updateData {
            it.copy(
                pinHash = null,
                pinSalt = null,
                biometricsEnabled = false,
                failedPinAttempts = 0,
                lockoutUntilMs = 0,
            )
        }
    }

    suspend fun setBiometricsEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(biometricsEnabled = enabled) }
    }

    /** A successful biometric check also clears the wrong-PIN counter. */
    suspend fun resetFailedAttempts() {
        dataStore.updateData { it.copy(failedPinAttempts = 0, lockoutUntilMs = 0) }
    }

    suspend fun verifyPin(pin: String): PinResult {
        val prefs = dataStore.data.first()
        val hash = prefs.pinHash ?: return PinResult.Success // nothing to protect
        val salt = prefs.pinSalt ?: return PinResult.Success

        val (result, newState) = PinLockoutPolicy.evaluate(
            state = PinAttemptState(prefs.failedPinAttempts, prefs.lockoutUntilMs),
            nowMs = clock(),
        ) {
            withContext(Dispatchers.Default) {
                PinHasher.verify(pin, salt.fromBase64(), hash.fromBase64())
            }
        }
        dataStore.updateData {
            it.copy(failedPinAttempts = newState.failedAttempts, lockoutUntilMs = newState.lockoutUntilMs)
        }
        return result
    }

    private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
    private fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)
}
