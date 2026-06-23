package com.subhranil.clouddnsmanager.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.subhranil.clouddnsmanager.nav.NavigationRouter
import com.subhranil.clouddnsmanager.onboading.OnBoardingViewModel
import com.subhranil.clouddnsmanager.selectzones.SelectZoneViewModel
import com.subhranil.clouddnsmanager.start.StartViewModel
import com.subhranil.clouddnsmanager.storage.UserPreferences
import com.subhranil.clouddnsmanager.storage.UserPreferencesSerializer
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::StartViewModel)
    viewModelOf(::OnBoardingViewModel)
    viewModelOf(::SelectZoneViewModel)
    singleOf(::NavigationRouter)
    single<DataStore<UserPreferences>> {
        DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            produceFile = { androidContext().dataStoreFile("user-preferences") }
        )
    }
}