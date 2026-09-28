package com.subhranil.clouddnsmanager.dns.di

import com.subhranil.clouddnsmanager.dns.DnsRecordViewModel
import com.subhranil.clouddnsmanager.dns.DnsRecordsChangeNotifier
import com.subhranil.clouddnsmanager.dns.edit.DnsRecordEditorViewModel
import com.subhranil.clouddnsmanager.dns.nav.DnsDestination
import com.subhranil.clouddnsmanager.security.AuthGate
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin wiring for the DNS feature. Loaded from MyApp alongside the other modules. */
val dnsModule = module {
    single { DnsRecordsChangeNotifier() }

    viewModel { (zoneId: String) ->
        DnsRecordViewModel(
            zoneId = zoneId,
            router = get(),
            sessionManager = get(),
            authGate = get<AuthGate>(),
            lockManager = get(),
            noteRepository = get(),
            changeNotifier = get(),
        )
    }
    viewModel { (destination: DnsDestination.Editor) ->
        DnsRecordEditorViewModel(
            destination = destination,
            router = get(),
            sessionManager = get(),
            authGate = get<AuthGate>(),
            lockManager = get(),
            changeNotifier = get(),
        )
    }
}
