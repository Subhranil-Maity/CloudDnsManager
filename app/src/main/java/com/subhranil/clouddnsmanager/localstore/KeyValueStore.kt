package com.subhranil.clouddnsmanager.localstore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/**
 * Small local key-value store shared by every feature (notes, locks, ...).
 * Values never leave the device. The production implementation is encrypted at rest.
 */
interface KeyValueStore {
    fun observe(key: String): Flow<String?>

    /** All entries whose key starts with [prefix], keyed by the full key. */
    fun observeByPrefix(prefix: String): Flow<Map<String, String>>

    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)

    /** Wipe everything (used on logout). */
    suspend fun clear()
}

@Serializable
data class LocalStoreData(
    val entries: Map<String, String> = emptyMap(),
)

/** [KeyValueStore] backed by an encrypted DataStore file (see localstore/di). */
class EncryptedKeyValueStore(
    private val dataStore: DataStore<LocalStoreData>,
) : KeyValueStore {

    override fun observe(key: String): Flow<String?> =
        dataStore.data.map { it.entries[key] }.distinctUntilChanged()

    override fun observeByPrefix(prefix: String): Flow<Map<String, String>> =
        dataStore.data.map { data -> data.entries.filterKeys { it.startsWith(prefix) } }.distinctUntilChanged()

    override suspend fun get(key: String): String? = dataStore.data.first().entries[key]

    override suspend fun put(key: String, value: String) {
        dataStore.updateData { it.copy(entries = it.entries + (key to value)) }
    }

    override suspend fun remove(key: String) {
        dataStore.updateData { it.copy(entries = it.entries - key) }
    }

    override suspend fun clear() {
        dataStore.updateData { LocalStoreData() }
    }
}
