package com.subhranil.clouddnsmanager.dns

import com.subhranil.clouddnsmanager.dns.record.displayComment
import com.subhranil.clouddnsmanager.dns.record.managedReason
import com.subhranil.clouddnsmanager.http.cfJson
import com.subhranil.clouddnsmanager.localstore.lock.LockMarker
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsRecordMeta
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsRecordRulesTest {

    private fun record(meta: DnsRecordMeta = DnsRecordMeta(), locked: Boolean = false, comment: String? = null) =
        DnsRecord(
            id = "r1", name = "example.com", type = DnsRecordType.MX, content = "route1.mx.cloudflare.net",
            ttl = 1, locked = locked, meta = meta, comment = comment,
        )

    @Test
    fun `plain records are not managed`() {
        assertNull(managedReason(record()))
        // auto_added only means "imported when the zone was set up"
        assertNull(managedReason(record(DnsRecordMeta(autoAdded = true))))
    }

    @Test
    fun `each cloudflare flag gives a reason`() {
        assertEquals("Managed by Cloudflare Email Routing", managedReason(record(DnsRecordMeta(emailRouting = true))))
        assertEquals("Managed by a Cloudflare Tunnel", managedReason(record(DnsRecordMeta(managedByArgoTunnel = true))))
        assertEquals("Managed by a Cloudflare app", managedReason(record(DnsRecordMeta(managedByApps = true))))
        assertEquals("Read-only record managed by Cloudflare", managedReason(record(DnsRecordMeta(readOnly = true))))
        assertEquals("Locked by Cloudflare", managedReason(record(locked = true)))
    }

    @Test
    fun `most specific product wins`() {
        assertEquals(
            "Managed by Cloudflare Email Routing",
            managedReason(record(DnsRecordMeta(emailRouting = true, readOnly = true), locked = true)),
        )
    }

    @Test
    fun `user lock marker does not make a record managed`() {
        assertNull(managedReason(record(comment = LockMarker.add("office"))))
    }

    @Test
    fun `meta flags parse from cloudflare json and unknown keys are ignored`() {
        val json = """
            {"id":"r1","name":"example.com","type":"MX","content":"route1.mx.cloudflare.net","ttl":1,
             "comment":null,"tags":["a:b"],
             "meta":{"auto_added":false,"email_routing":true,"read_only":true,"shadowed_by":[],"new_flag":1}}
        """.trimIndent()
        val parsed = cfJson.decodeFromString(DnsRecord.serializer(), json)
        assertEquals(listOf("a:b"), parsed.tags)
        assertEquals("Managed by Cloudflare Email Routing", managedReason(parsed))
    }

    @Test
    fun `records without meta parse with defaults`() {
        val json = """{"id":"r1","name":"a.example.com","type":"A","content":"192.0.2.1","ttl":1}"""
        val parsed = cfJson.decodeFromString(DnsRecord.serializer(), json)
        assertEquals(DnsRecordMeta(), parsed.meta)
        assertNull(managedReason(parsed))
    }

    @Test
    fun `display comment strips the lock marker`() {
        assertEquals("office", record(comment = LockMarker.add("office")).displayComment())
        assertNull(record(comment = LockMarker.TOKEN).displayComment())
        assertNull(record(comment = null).displayComment())
    }
}
