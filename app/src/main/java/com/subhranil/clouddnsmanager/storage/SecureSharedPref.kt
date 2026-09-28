package com.subhranil.clouddnsmanager.storage

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val token: String? = null,
    // --- App lock (see security/AppLockRepository) ---
    /** Base64 PBKDF2 hash of the app PIN; the PIN itself is never stored. */
    val pinHash: String? = null,
    /** Base64 random salt used for [pinHash]. */
    val pinSalt: String? = null,
    val biometricsEnabled: Boolean = false,
    val failedPinAttempts: Int = 0,
    val lockoutUntilMs: Long = 0,
)
/** Encrypted-at-rest serializer for the main preferences file (token, PIN hash, ...). */
val UserPreferencesSerializer: Serializer<UserPreferences> =
    EncryptedJsonSerializer(UserPreferences.serializer(), UserPreferences())
