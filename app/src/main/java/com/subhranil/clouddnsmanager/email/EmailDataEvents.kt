package com.subhranil.clouddnsmanager.email

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells the Email screens that Cloudflare data changed on another screen (e.g. an alias was
 * created on the Create Alias screen), so lists still on the back stack reload.
 * A Koin singleton inside the email module.
 */
class EmailDataEvents {
    private val _aliasesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val aliasesChanged: SharedFlow<Unit> = _aliasesChanged.asSharedFlow()

    private val _addressesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val addressesChanged: SharedFlow<Unit> = _addressesChanged.asSharedFlow()

    fun notifyAliasesChanged() {
        _aliasesChanged.tryEmit(Unit)
    }

    fun notifyAddressesChanged() {
        _addressesChanged.tryEmit(Unit)
    }
}
