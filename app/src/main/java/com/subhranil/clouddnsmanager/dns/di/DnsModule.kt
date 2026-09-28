package com.subhranil.clouddnsmanager.dns.di

import com.subhranil.clouddnsmanager.dns.DnsRecordViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin wiring for the DNS feature. Loaded from MyApp alongside the other modules. */
val dnsModule = module {
    viewModel { (zoneId: String) -> DnsRecordViewModel(zoneId, get(), get()) }
}
