package com.subhranil.clouddnsmanager.storage

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class UserPreferences(
    val token: String? = null
)
object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences
        get() = UserPreferences()

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun readFrom(input: InputStream): UserPreferences {
        return withContext(Dispatchers.IO) {
            try {
                val encryptedBytesBase64 = input.use { it.readBytes() }
                if (encryptedBytesBase64.isEmpty()) return@withContext defaultValue

                val encryptedBytes = Base64.decode(encryptedBytesBase64)
                val decryptedBytes = Crypto.decrypt(encryptedBytes)

                Json.decodeFromString(UserPreferences.serializer(), decryptedBytes.decodeToString())
            } catch (e: Exception) {
                e.printStackTrace()
                defaultValue // Fallback seamlessly if decoding fails or keys rotate
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            val jsonString = Json.encodeToString(UserPreferences.serializer(), t)
            val encryptedBytes = Crypto.encrypt(jsonString.toByteArray(Charsets.UTF_8))
            val encryptedBytesBase64 = Base64.encodeToByteArray(encryptedBytes)

            output.use {
                it.write(encryptedBytesBase64)
            }
        }
    }
}