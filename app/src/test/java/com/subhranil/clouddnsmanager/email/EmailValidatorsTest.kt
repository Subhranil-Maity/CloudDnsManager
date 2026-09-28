package com.subhranil.clouddnsmanager.email

import com.subhranil.clouddnsmanager.email.domain.EmailValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorsTest {

    @Test
    fun `accepts typical local parts and normalises them`() {
        listOf("hello", "quiet-river-4821", "first.last", "a_b", "shop+news", "x").forEach {
            assertTrue(it, EmailValidators.validateLocalPart(it).isValid)
        }
        val result = EmailValidators.validateLocalPart("  Hello.World ")
        assertTrue(result.isValid)
        assertEquals("hello.world", result.value)
    }

    @Test
    fun `rejects bad local parts`() {
        listOf(
            "",
            "   ",
            "a@b",
            "has space",
            ".start",
            "end.",
            "two..dots",
            "bad(char)",
            "comma,here",
            "ümlaut",
            "a".repeat(65),
        ).forEach { assertFalse("should reject '$it'", EmailValidators.validateLocalPart(it).isValid) }
    }

    @Test
    fun `local part limit is 64 characters`() {
        assertTrue(EmailValidators.validateLocalPart("a".repeat(64)).isValid)
        assertFalse(EmailValidators.validateLocalPart("a".repeat(65)).isValid)
    }

    @Test
    fun `full address must fit Cloudflare's 90 character limit`() {
        val zone = "d".repeat(40) + ".com" // 44 chars
        assertTrue(EmailValidators.validateLocalPart("a".repeat(45), zone).isValid) // 45 + 1 + 44 = 90
        assertNotNull(EmailValidators.validateLocalPart("a".repeat(46), zone).error)
    }

    @Test
    fun `accepts valid emails`() {
        listOf("me@example.com", "first.last+tag@mail.example.co.uk", "  a@b.io  ").forEach {
            assertTrue(it, EmailValidators.validateEmail(it).isValid)
        }
        assertEquals("a@b.io", EmailValidators.validateEmail("  a@b.io  ").value)
    }

    @Test
    fun `rejects invalid emails`() {
        listOf(
            "",
            "plainaddress",
            "@example.com",
            "me@",
            "me@example",
            "me@-example.com",
            "me@example..com",
            "me@exa mple.com",
            "two@@example.com",
            "me..x@example.com",
            "me@example.c",
        ).forEach { assertFalse("should reject '$it'", EmailValidators.validateEmail(it).isValid) }
    }
}
