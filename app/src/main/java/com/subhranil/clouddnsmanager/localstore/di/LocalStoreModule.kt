package com.subhranil.clouddnsmanager.localstore.di

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.subhranil.clouddnsmanager.localstore.EncryptedKeyValueStore
import com.subhranil.clouddnsmanager.localstore.KeyValueStore
import com.subhranil.clouddnsmanager.localstore.LocalStoreData
import com.subhranil.clouddnsmanager.localstore.lock.ItemLockManager
import com.subhranil.clouddnsmanager.localstore.notes.NoteRepository
import com.subhranil.clouddnsmanager.security.AuthGate
import com.subhranil.clouddnsmanager.storage.EncryptedJsonSerializer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Shared local storage used by every feature: notes and the central lock manager. */
val localStoreModule = module {
    // The DataStore is created here rather than registered on its own: Koin can't tell
    // DataStore<LocalStoreData> from DataStore<UserPreferences> (generics are erased).
    single<KeyValueStore> {
        EncryptedKeyValueStore(
            DataStoreFactory.create(
                serializer = EncryptedJsonSerializer(LocalStoreData.serializer(), LocalStoreData()),
                produceFile = { androidContext().dataStoreFile("local-store") },
            )
        )
    }
    single { NoteRepository(get()) }
    single { ItemLockManager(get(), get<AuthGate>()) }
}
