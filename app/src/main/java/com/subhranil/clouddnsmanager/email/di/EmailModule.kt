package com.subhranil.clouddnsmanager.email.di

import com.subhranil.clouddnsmanager.email.home.EmailHomeViewModel
import com.subhranil.clouddnsmanager.email.nav.EmailDestination
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin wiring for the Email feature. Loaded from MyApp alongside the other modules. */
val emailModule = module {
    viewModel { (destination: EmailDestination.Home) -> EmailHomeViewModel(destination, get(), get()) }
}
