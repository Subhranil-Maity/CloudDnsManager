package com.subhranil.clouddnsmanager.localstore.lock

import com.subhranil.clouddnsmanager.localstore.ItemKey
import com.subhranil.clouddnsmanager.localstore.KeyValueStore
import com.subhranil.clouddnsmanager.security.Authorizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the UI should do with an item. */
sealed interface LockStatus {
    data object Unlocked : LockStatus

    /** Locked by the user in this app; can be unlocked after authentication. */
    data object UserLocked : LockStatus

    /** Locked by Cloudflare itself (e.g. Email Routing records); cannot be unlocked in the app. */
    data class Managed(val reason: String) : LockStatus

    val isEditable: Boolean get() = this == Unlocked
}

sealed interface LockResult {
    data object Success : LockResult
    /** The user cancelled or failed authentication; nothing changed. */
    data object NotAuthorized : LockResult
    /** Syncing to Cloudflare failed; nothing changed locally either. */
    data class Failed(val error: Throwable) : LockResult
}

/**
 * Optional hook that mirrors a lock to Cloudflare (e.g. the DNS feature writes
 * [LockMarker] into the record's comment). Implemented per feature, passed per call.
 */
fun interface RemoteLockSync {
    suspend fun setRemoteLocked(locked: Boolean)
}

/**
 * The single place that decides whether an item is locked, for every feature.
 *
 * - A locked item must not be edited or deleted: check [isLocked] / [status] first.
 * - Locking adds protection, so it needs no authentication.
 * - Unlocking removes protection, so it always goes through the [Authorizer] (AuthGate).
 * - With a [RemoteLockSync], Cloudflare is updated first and the local state only changes
 *   if that succeeds, so the two never disagree after a failure.
 */
class ItemLockManager(
    private val store: KeyValueStore,
    private val authorizer: Authorizer,
) {
    fun observeLocked(item: ItemKey): Flow<Boolean> =
        store.observe(storeKey(item)).map { it != null }

    /** Locked items among those whose key starts with [itemPrefix] (e.g. [ItemKey.dnsZonePrefix]). */
    fun observeLockedItems(itemPrefix: String): Flow<Set<ItemKey>> =
        store.observeByPrefix(PREFIX + itemPrefix).map { entries ->
            entries.keys.map { ItemKey(it.removePrefix(PREFIX)) }.toSet()
        }

    suspend fun isLocked(item: ItemKey): Boolean = store.get(storeKey(item)) != null

    /**
     * Combines the user's lock with a Cloudflare-managed lock. [managedReason] non-null means
     * Cloudflare itself protects the item, which always wins.
     */
    fun status(userLocked: Boolean, managedReason: String? = null): LockStatus = when {
        managedReason != null -> LockStatus.Managed(managedReason)
        userLocked -> LockStatus.UserLocked
        else -> LockStatus.Unlocked
    }

    suspend fun lock(item: ItemKey, remote: RemoteLockSync? = null): LockResult {
        try {
            remote?.setRemoteLocked(true)
        } catch (e: Exception) {
            return LockResult.Failed(e)
        }
        store.put(storeKey(item), LOCKED)
        return LockResult.Success
    }

    suspend fun unlock(item: ItemKey, reason: String, remote: RemoteLockSync? = null): LockResult {
        if (!authorizer.authorize(reason)) return LockResult.NotAuthorized
        try {
            remote?.setRemoteLocked(false)
        } catch (e: Exception) {
            return LockResult.Failed(e)
        }
        store.remove(storeKey(item))
        return LockResult.Success
    }

    /**
     * Adopt the lock state read from Cloudflare (e.g. [LockMarker] found in a record's
     * comment) when a list loads. Cloudflare is the source of truth for synced items.
     */
    suspend fun adoptRemoteState(item: ItemKey, remoteLocked: Boolean) {
        if (remoteLocked) store.put(storeKey(item), LOCKED) else store.remove(storeKey(item))
    }

    /** Call when the underlying item is deleted. */
    suspend fun forget(item: ItemKey) = store.remove(storeKey(item))

    private fun storeKey(item: ItemKey) = PREFIX + item.value

    private companion object {
        const val PREFIX = "lock:"
        const val LOCKED = "1"
    }
}
