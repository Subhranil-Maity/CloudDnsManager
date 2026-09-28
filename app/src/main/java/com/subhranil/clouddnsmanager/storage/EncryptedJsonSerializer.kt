package com.subhranil.clouddnsmanager.storage

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * DataStore serializer that stores [T] as JSON, AES-encrypted with the Android Keystore key
 * ([Crypto]) and Base64-encoded. Unreadable data (corruption, key loss after a restore)
 * falls back to [defaultValue] instead of crashing.
 */
class EncryptedJsonSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
) : Serializer<T> {

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun readFrom(input: InputStream): T = withContext(Dispatchers.IO) {
        try {
            val encryptedBase64 = input.use { it.readBytes() }
            if (encryptedBase64.isEmpty()) return@withContext defaultValue
            val decrypted = Crypto.decrypt(Base64.decode(encryptedBase64))
            json.decodeFromString(serializer, decrypted.decodeToString())
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun writeTo(t: T, output: OutputStream) = withContext(Dispatchers.IO) {
        val encrypted = Crypto.encrypt(json.encodeToString(serializer, t).toByteArray(Charsets.UTF_8))
        output.use { it.write(Base64.encodeToByteArray(encrypted)) }
    }
}
