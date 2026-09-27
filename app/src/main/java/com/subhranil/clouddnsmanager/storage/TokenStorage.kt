package com.subhranil.clouddnsmanager.storage

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

interface TokenStorage {
    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    /** Wipe the token and every other stored preference (PIN, biometrics, lockout state). */
    suspend fun clearAll()
}

class DataStoreTokenStorage(
    private val dataStore: DataStore<UserPreferences>
) : TokenStorage {

    override suspend fun getToken(): String? {
        return dataStore.data.first().token
    }

    override suspend fun saveToken(token: String) {
        dataStore.updateData { currentPrefs ->
            currentPrefs.copy(token = token)
        }
    }

    override suspend fun clearAll() {
        dataStore.updateData { UserPreferences() }
    }
}