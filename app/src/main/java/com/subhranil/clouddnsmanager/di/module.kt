package com.subhranil.clouddnsmanager.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.http.SessionManager
import com.subhranil.clouddnsmanager.http.SessionState
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.onboading.OnBoardingViewModel
import com.subhranil.clouddnsmanager.selectzones.SelectZoneViewModel
import com.subhranil.clouddnsmanager.start.StartViewModel
import com.subhranil.clouddnsmanager.storage.DataStoreTokenStorage
import com.subhranil.clouddnsmanager.storage.TokenStorage
import com.subhranil.clouddnsmanager.storage.UserPreferences
import com.subhranil.clouddnsmanager.storage.UserPreferencesSerializer
import com.subhranil.clouddnsmanager.dns.DnsRecordViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // 1. ViewModels
    viewModelOf(::StartViewModel)
    viewModelOf(::OnBoardingViewModel)
    viewModelOf(::SelectZoneViewModel) // Koin automatically injects CloudflareClient here now!
    viewModelOf(::DnsRecordViewModel)

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
    factory<CloudflareClient> {
        val state = get<SessionManager>().sessionState.value
        if (state is SessionState.Authenticated) {
            state.client
        } else {
            // This will NEVER trigger unless you accidentally write bad navigation code.
            // It acts as a safety guard for you while coding.
            throw IllegalStateException("CloudflareClient requested but user is not authenticated!")
        }
    }
}