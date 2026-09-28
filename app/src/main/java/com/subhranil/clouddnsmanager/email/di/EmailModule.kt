package com.subhranil.clouddnsmanager.email.di

import com.subhranil.clouddnsmanager.email.EmailDataEvents
import com.subhranil.clouddnsmanager.email.EmailZone
import com.subhranil.clouddnsmanager.email.activity.ActivityViewModel
import com.subhranil.clouddnsmanager.email.addresses.AddressListViewModel
import com.subhranil.clouddnsmanager.email.aliases.AliasListViewModel
import com.subhranil.clouddnsmanager.email.aliases.create.CreateAliasViewModel
import com.subhranil.clouddnsmanager.email.aliases.detail.AliasDetailViewModel
import com.subhranil.clouddnsmanager.email.domain.RandomAliasGenerator
import com.subhranil.clouddnsmanager.email.home.EmailHomeViewModel
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin wiring for the Email feature. Loaded from MyApp alongside the other modules. */
val emailModule = module {
    // Cross-screen "data changed" signal and the alias generator (SecureRandom)
    single { EmailDataEvents() }
    factory { RandomAliasGenerator() }

    viewModel { (destination: EmailDestination.Home) -> EmailHomeViewModel(destination, get(), get()) }
    viewModel { (zone: EmailZone) -> AliasListViewModel(zone, get(), get(), get(), get(), get(), get()) }
    viewModel { (zone: EmailZone) -> AddressListViewModel(zone, get(), get(), get(), get(), get()) }
    viewModel { (zoneId: String, aliasFilter: String) -> ActivityViewModel(zoneId, aliasFilter, get()) }
    viewModel { (destination: EmailDestination.CreateAlias) ->
        CreateAliasViewModel(destination, get(), get(), get(), get(), get())
    }
    viewModel { (destination: EmailDestination.AliasDetail) ->
        AliasDetailViewModel(destination, get(), get(), get(), get(), get(), get())
    }
}
