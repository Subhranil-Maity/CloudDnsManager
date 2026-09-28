package com.subhranil.clouddnsmanager.dns

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells the records list that a zone's records changed somewhere else (e.g. the editor
 * saved a record and popped back), so it reloads from Cloudflare. Koin singleton.
 */
class DnsRecordsChangeNotifier {
    private val _changes = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Emits the zone ID whose records changed. */
    val changes: SharedFlow<String> = _changes.asSharedFlow()

    fun notifyChanged(zoneId: String) {
        _changes.tryEmit(zoneId)
    }
}
