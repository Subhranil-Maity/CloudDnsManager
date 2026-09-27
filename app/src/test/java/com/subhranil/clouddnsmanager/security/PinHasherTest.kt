package com.subhranil.clouddnsmanager.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `same pin and salt verifies`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("123456", salt)
        assertTrue(PinHasher.verify("123456", salt, hash))
    }

    @Test
    fun `different pin does not verify`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("123456", salt)
        assertFalse(PinHasher.verify("123457", salt, hash))
    }

    @Test
    fun `different salt produces a different hash`() {
        val a = PinHasher.hash("123456", PinHasher.newSalt())
        val b = PinHasher.hash("123456", PinHasher.newSalt())
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `hashing is deterministic for a given salt`() {
        val salt = PinHasher.newSalt()
        assertArrayEquals(PinHasher.hash("000000", salt), PinHasher.hash("000000", salt))
    }

    @Test
    fun `hash does not contain the raw pin`() {
        val hash = PinHasher.hash("123456", PinHasher.newSalt())
        assertEquals(32, hash.size) // 256-bit output
        assertFalse(String(hash, Charsets.ISO_8859_1).contains("123456"))
    }

    @Test
    fun `only six digit pins are valid`() {
        assertTrue(PinHasher.isValidPin("012345"))
        assertFalse(PinHasher.isValidPin("12345"))
        assertFalse(PinHasher.isValidPin("1234567"))
        assertFalse(PinHasher.isValidPin("12a456"))
    }
}
