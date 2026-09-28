package com.subhranil.clouddnsmanager

import android.app.Application
import com.subhranil.clouddnsmanager.di.appModule
import com.subhranil.clouddnsmanager.dns.di.dnsModule
import com.subhranil.clouddnsmanager.email.di.emailModule
import com.subhranil.clouddnsmanager.localstore.di.localStoreModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            // Shared app wiring, shared local storage, then one module per feature package
            modules(appModule, localStoreModule, dnsModule, emailModule)
        }
    }
}
