package com.subhranil.clouddnsmanager.localstore

import com.subhranil.clouddnsmanager.localstore.lock.ItemLockManager
import com.subhranil.clouddnsmanager.localstore.lock.LockResult
import com.subhranil.clouddnsmanager.localstore.lock.LockStatus
import com.subhranil.clouddnsmanager.localstore.lock.RemoteLockSync
import com.subhranil.clouddnsmanager.security.Authorizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory [KeyValueStore] for tests. */
class FakeKeyValueStore : KeyValueStore {
    val entries = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun observe(key: String): Flow<String?> = entries.map { it[key] }
    override fun observeByPrefix(prefix: String): Flow<Map<String, String>> =
        entries.map { all -> all.filterKeys { it.startsWith(prefix) } }
    override suspend fun get(key: String) = entries.value[key]
    override suspend fun put(key: String, value: String) { entries.value = entries.value + (key to value) }
    override suspend fun remove(key: String) { entries.value = entries.value - key }
    override suspend fun clear() { entries.value = emptyMap() }
}

class ItemLockManagerTest {

    private val item = ItemKey.dnsRecord("zone1", "rec1")
    private val store = FakeKeyValueStore()

    private fun manager(authorized: Boolean) = ItemLockManager(store, Authorizer { authorized })

    @Test
    fun `lock needs no authentication`() = runBlocking {
        var asked = false
        val lockManager = ItemLockManager(store, Authorizer { asked = true; false })
        assertEquals(LockResult.Success, lockManager.lock(item))
        assertTrue(lockManager.isLocked(item))
        assertFalse(asked)
    }

    @Test
    fun `unlock is refused without authentication`() = runBlocking {
        val lockManager = manager(authorized = false)
        lockManager.lock(item)
        assertEquals(LockResult.NotAuthorized, lockManager.unlock(item, "test"))
        assertTrue(lockManager.isLocked(item))
    }

    @Test
    fun `unlock succeeds after authentication`() = runBlocking {
        val lockManager = manager(authorized = true)
        lockManager.lock(item)
        assertEquals(LockResult.Success, lockManager.unlock(item, "test"))
        assertFalse(lockManager.isLocked(item))
    }

    @Test
    fun `remote failure leaves local state untouched`() = runBlocking {
        val lockManager = manager(authorized = true)
        val failing = RemoteLockSync { error("network down") }
        val result = lockManager.lock(item, failing)
        assertTrue(result is LockResult.Failed)
        assertFalse(lockManager.isLocked(item))
    }

    @Test
    fun `remote is updated before local`() = runBlocking {
        val lockManager = manager(authorized = true)
        var remoteLocked: Boolean? = null
        lockManager.lock(item) { remoteLocked = it }
        assertEquals(true, remoteLocked)
        lockManager.unlock(item, "test") { remoteLocked = it }
        assertEquals(false, remoteLocked)
    }

    @Test
    fun `adopting remote state overrides local`() = runBlocking {
        val lockManager = manager(authorized = false)
        lockManager.adoptRemoteState(item, remoteLocked = true)
        assertTrue(lockManager.isLocked(item))
        lockManager.adoptRemoteState(item, remoteLocked = false)
        assertFalse(lockManager.isLocked(item))
    }

    @Test
    fun `observeLockedItems filters by prefix`() = runBlocking {
        val lockManager = manager(authorized = true)
        val other = ItemKey.emailRule("zone1", "rule1")
        lockManager.lock(item)
        lockManager.lock(other)
        assertEquals(setOf(item), lockManager.observeLockedItems(ItemKey.dnsZonePrefix("zone1")).first())
    }

    @Test
    fun `cloudflare managed lock wins over user state`() {
        val lockManager = manager(authorized = true)
        assertEquals(LockStatus.Managed("Email Routing"), lockManager.status(userLocked = false, managedReason = "Email Routing"))
        assertEquals(LockStatus.UserLocked, lockManager.status(userLocked = true))
        assertEquals(LockStatus.Unlocked, lockManager.status(userLocked = false))
        assertFalse(LockStatus.UserLocked.isEditable)
    }
}
