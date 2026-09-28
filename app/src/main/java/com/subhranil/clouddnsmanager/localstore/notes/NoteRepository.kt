package com.subhranil.clouddnsmanager.localstore.notes

import com.subhranil.clouddnsmanager.localstore.ItemKey
import com.subhranil.clouddnsmanager.localstore.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Private, device-only notes attached to any [ItemKey] (DNS record, email alias, address).
 * Notes are local annotations and never change anything on Cloudflare, so editing them
 * does not go through the AuthGate.
 */
class NoteRepository(private val store: KeyValueStore) {

    fun observeNote(item: ItemKey): Flow<String?> = store.observe(storeKey(item))

    /** All notes for items whose key starts with [itemPrefix] (e.g. [ItemKey.dnsZonePrefix]). */
    fun observeNotes(itemPrefix: String): Flow<Map<ItemKey, String>> =
        store.observeByPrefix(PREFIX + itemPrefix).map { entries ->
            entries.mapKeys { (key, _) -> ItemKey(key.removePrefix(PREFIX)) }
        }

    /** Saves the note; a null or blank note deletes it. */
    suspend fun setNote(item: ItemKey, note: String?) {
        val trimmed = note?.trim()
        if (trimmed.isNullOrEmpty()) store.remove(storeKey(item)) else store.put(storeKey(item), trimmed)
    }

    /** Call when the underlying item is deleted so orphaned notes don't pile up. */
    suspend fun deleteNote(item: ItemKey) = store.remove(storeKey(item))

    private fun storeKey(item: ItemKey) = PREFIX + item.value

    private companion object {
        const val PREFIX = "note:"
    }
}
