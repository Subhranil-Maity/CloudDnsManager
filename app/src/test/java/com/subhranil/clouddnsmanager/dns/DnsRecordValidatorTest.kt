package com.subhranil.clouddnsmanager.dns

import com.subhranil.clouddnsmanager.dns.edit.DnsField
import com.subhranil.clouddnsmanager.dns.edit.DnsRecordForm
import com.subhranil.clouddnsmanager.dns.validation.DnsRecordValidator
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsRecordValidatorTest {

    private fun errors(form: DnsRecordForm) = DnsRecordValidator.validate(form)

    @Test
    fun `ipv4 addresses`() {
        listOf("192.0.2.1", "0.0.0.0", "255.255.255.255", "10.0.0.10").forEach {
            assertTrue(it, DnsRecordValidator.isIpv4(it))
        }
        listOf("256.1.1.1", "1.2.3", "1.2.3.4.5", "01.2.3.4", "a.b.c.d", "", "1..2.3", " 1.2.3.4").forEach {
            assertFalse(it, DnsRecordValidator.isIpv4(it))
        }
    }

    @Test
    fun `ipv6 addresses`() {
        listOf(
            "2001:db8::1", "::", "::1", "fe80::", "2001:0db8:0000:0000:0000:ff00:0042:8329",
            "::ffff:192.0.2.1", "1:2:3:4:5:6:7:8", "1:2:3:4:5:6:1.2.3.4", "ABCD::ef",
        ).forEach { assertTrue(it, DnsRecordValidator.isIpv6(it)) }
        listOf(
            "", "1:2:3:4:5:6:7", "1:2:3:4:5:6:7:8:9", "2001:db8::1::2", "12345::", "g::1",
            ":1:2:3:4:5:6:7", "1:::2", "fe80::1%eth0", "192.0.2.1", "::ffff:999.0.2.1",
            "1:2:3:4:5:6:7:8::",
        ).forEach { assertFalse(it, DnsRecordValidator.isIpv6(it)) }
    }

    @Test
    fun `hostnames and record names`() {
        assertTrue(DnsRecordValidator.isHostname("mail.example.com"))
        assertTrue(DnsRecordValidator.isHostname("mail.example.com."))
        assertTrue(DnsRecordValidator.isHostname("sel._domainkey.example.com"))
        assertFalse(DnsRecordValidator.isHostname("localhost"))
        assertFalse(DnsRecordValidator.isHostname("-bad.example.com"))
        assertFalse(DnsRecordValidator.isHostname("bad-.example.com"))
        assertFalse(DnsRecordValidator.isHostname("a..b"))
        assertFalse(DnsRecordValidator.isHostname("has space.com"))
        assertFalse(DnsRecordValidator.isHostname("a".repeat(64) + ".com"))

        assertTrue(DnsRecordValidator.isRecordName("@"))
        assertTrue(DnsRecordValidator.isRecordName("www"))
        assertTrue(DnsRecordValidator.isRecordName("*.dev"))
        assertTrue(DnsRecordValidator.isRecordName("_dmarc.example.com"))
        assertFalse(DnsRecordValidator.isRecordName("dev.*"))
        assertFalse(DnsRecordValidator.isRecordName("bad name"))
    }

    @Test
    fun `valid simple records have no errors`() {
        assertEquals(emptyMap<DnsField, String>(), errors(DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1")))
        assertEquals(emptyMap<DnsField, String>(), errors(DnsRecordForm(DnsRecordType.AAAA, name = "www", content = "2001:db8::1")))
        assertEquals(emptyMap<DnsField, String>(), errors(DnsRecordForm(DnsRecordType.CNAME, name = "blog", content = "example.net")))
        assertEquals(emptyMap<DnsField, String>(), errors(DnsRecordForm(DnsRecordType.TXT, name = "@", content = "v=spf1 -all")))
        assertEquals(emptyMap<DnsField, String>(), errors(DnsRecordForm(DnsRecordType.NS, name = "sub", content = "ns1.example.net")))
        assertEquals(emptyMap<DnsField, String>(), errors(DnsRecordForm(DnsRecordType.MX, name = "@", content = "mx.example.net", priority = "10")))
    }

    @Test
    fun `required fields`() {
        val result = errors(DnsRecordForm(DnsRecordType.A, name = "", content = ""))
        assertEquals(setOf(DnsField.NAME, DnsField.CONTENT), result.keys)
        assertTrue(DnsField.CONTENT in errors(DnsRecordForm(DnsRecordType.TXT, name = "@", content = "  ")))
    }

    @Test
    fun `wrong address family is rejected`() {
        assertTrue(DnsField.CONTENT in errors(DnsRecordForm(DnsRecordType.A, name = "@", content = "2001:db8::1")))
        assertTrue(DnsField.CONTENT in errors(DnsRecordForm(DnsRecordType.AAAA, name = "@", content = "192.0.2.1")))
    }

    @Test
    fun `ttl range`() {
        val base = DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1")
        assertFalse(DnsField.TTL in errors(base.copy(ttl = 1)))
        assertFalse(DnsField.TTL in errors(base.copy(ttl = 3600)))
        assertFalse(DnsField.TTL in errors(base.copy(ttl = 86400)))
        assertTrue(DnsField.TTL in errors(base.copy(ttl = 0)))
        assertTrue(DnsField.TTL in errors(base.copy(ttl = 2)))
        assertTrue(DnsField.TTL in errors(base.copy(ttl = 86401)))
    }

    @Test
    fun `mx priority range`() {
        val base = DnsRecordForm(DnsRecordType.MX, name = "@", content = "mx.example.net")
        assertFalse(DnsField.PRIORITY in errors(base.copy(priority = "0")))
        assertFalse(DnsField.PRIORITY in errors(base.copy(priority = "65535")))
        assertTrue(DnsField.PRIORITY in errors(base.copy(priority = "65536")))
        assertTrue(DnsField.PRIORITY in errors(base.copy(priority = "-1")))
        assertTrue(DnsField.PRIORITY in errors(base.copy(priority = "")))
        assertTrue(DnsField.PRIORITY in errors(base.copy(priority = "ten")))
    }

    @Test
    fun `srv fields`() {
        val valid = DnsRecordForm(
            DnsRecordType.SRV, name = "", srvService = "_sip", srvProto = "_tcp",
            srvPriority = "10", srvWeight = "5", srvPort = "5060", srvTarget = "sip.example.com",
        )
        assertEquals(emptyMap<DnsField, String>(), errors(valid))
        assertEquals(emptyMap<DnsField, String>(), errors(valid.copy(srvTarget = ".")))

        val result = errors(valid.copy(srvService = "", srvPort = "70000", srvWeight = "x", srvTarget = "nohost"))
        assertEquals(setOf(DnsField.SRV_SERVICE, DnsField.SRV_PORT, DnsField.SRV_WEIGHT, DnsField.SRV_TARGET), result.keys)
    }

    @Test
    fun `caa fields`() {
        val valid = DnsRecordForm(DnsRecordType.CAA, name = "@", caaFlags = "0", caaTag = "issue", caaValue = "letsencrypt.org")
        assertEquals(emptyMap<DnsField, String>(), errors(valid))
        assertTrue(DnsField.CAA_FLAGS in errors(valid.copy(caaFlags = "256")))
        assertTrue(DnsField.CAA_TAG in errors(valid.copy(caaTag = "bogus")))
        assertTrue(DnsField.CAA_VALUE in errors(valid.copy(caaValue = " ")))
        assertTrue(DnsField.CAA_VALUE in errors(valid.copy(caaTag = "iodef", caaValue = "security@example.com")))
        assertFalse(DnsField.CAA_VALUE in errors(valid.copy(caaTag = "iodef", caaValue = "mailto:security@example.com")))
    }

    @Test
    fun `comment length`() {
        val base = DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1")
        assertFalse(DnsField.COMMENT in errors(base.copy(comment = "x".repeat(100))))
        assertTrue(DnsField.COMMENT in errors(base.copy(comment = "x".repeat(101))))
    }

    @Test
    fun `unsupported types are refused`() {
        assertTrue(errors(DnsRecordForm(DnsRecordType.TLSA, name = "@")).isNotEmpty())
    }

    @Test
    fun `non ascii names ask for punycode`() {
        val result = errors(DnsRecordForm(DnsRecordType.A, name = "bücher", content = "192.0.2.1"))
        assertTrue(result[DnsField.NAME]!!.contains("Punycode"))
    }
}
