package com.subhranil.clouddnsmanager.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.onboading.OnBoardingViewModel
import com.subhranil.clouddnsmanager.selectzones.SelectZoneViewModel
import com.subhranil.clouddnsmanager.storage.DataStoreTokenStorage
import com.subhranil.clouddnsmanager.storage.TokenStorage
import com.subhranil.clouddnsmanager.storage.UserPreferences
import com.subhranil.clouddnsmanager.storage.UserPreferencesSerializer
import com.subhranil.clouddnsmanager.dns.DnsRecordViewModel
import com.subhranil.clouddnsmanager.lock.LockViewModel
import com.subhranil.clouddnsmanager.security.AppLockManager
import com.subhranil.clouddnsmanager.security.AppLockRepository
import com.subhranil.clouddnsmanager.security.AuthGate
import com.subhranil.clouddnsmanager.security.BiometricAuthenticator
import com.subhranil.clouddnsmanager.security.settings.SecuritySettingsViewModel
import com.subhranil.clouddnsmanager.security.setup.PinSetupViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // 1. ViewModels
    viewModelOf(::OnBoardingViewModel)
    viewModelOf(::SelectZoneViewModel)
    viewModel { (zoneId: String) -> DnsRecordViewModel(zoneId, get(), get()) }
    viewModelOf(::LockViewModel)
    viewModelOf(::SecuritySettingsViewModel)
    viewModel { (changing: Boolean) -> PinSetupViewModel(changing, get(), get()) }

    // 2. Navigation
    singleOf(::NavigationRouter)

    // 3. DataStore Secure Engine
    single<DataStore<UserPreferences>> {
        DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            produceFile = { androidContext().dataStoreFile("user-preferences") }
        )
    }

    // 4. Token Storage Wrapper
    single<TokenStorage> { DataStoreTokenStorage(get()) }

    // 5. Shared Session Manager
    single { SessionManager(get()) }

    // 6. App lock (optional PIN + biometrics) and the destructive-action AuthGate
    single { AppLockRepository(get()) }
    single { AppLockManager(get()) }
    single { BiometricAuthenticator(androidContext()) }
    single { AuthGate() }
}