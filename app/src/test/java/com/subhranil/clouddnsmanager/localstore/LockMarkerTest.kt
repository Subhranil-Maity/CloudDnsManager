package com.subhranil.clouddnsmanager.localstore

import com.subhranil.clouddnsmanager.localstore.lock.LockMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockMarkerTest {

    @Test
    fun `adds marker to empty and existing text`() {
        assertEquals(LockMarker.TOKEN, LockMarker.add(null))
        assertEquals("${LockMarker.TOKEN} mail server", LockMarker.add("mail server"))
    }

    @Test
    fun `adding twice does not duplicate`() {
        val once = LockMarker.add("web")
        assertEquals(once, LockMarker.add(once))
    }

    @Test
    fun `remove restores original text`() {
        assertEquals("mail server", LockMarker.remove(LockMarker.add("mail server")))
        assertEquals("", LockMarker.remove(LockMarker.TOKEN))
    }

    @Test
    fun `detects marker`() {
        assertTrue(LockMarker.isLocked(LockMarker.add("x")))
        assertFalse(LockMarker.isLocked("x"))
        assertFalse(LockMarker.isLocked(null))
    }

    @Test
    fun `respects max length by shortening existing text`() {
        val result = LockMarker.add("a".repeat(200), maxLength = 100)
        assertEquals(100, result.length)
        assertTrue(result.startsWith(LockMarker.TOKEN))
    }
}
