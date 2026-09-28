package com.subhranil.clouddnsmanager.dns

import com.subhranil.clouddnsmanager.dns.edit.DnsRecordForm
import com.subhranil.clouddnsmanager.dns.edit.DnsRecordRequestBuilder
import com.subhranil.clouddnsmanager.http.cfJson
import com.subhranil.clouddnsmanager.localstore.lock.LockMarker
import com.subhranil.clouddnsmanager.models.dns.DnsCommentUpdate
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsRecordData
import com.subhranil.clouddnsmanager.models.dns.DnsRecordRequest
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsRecordRequestBuilderTest {

    private val zone = "example.com"

    private fun build(form: DnsRecordForm, originalComment: String? = null) =
        DnsRecordRequestBuilder.build(form, zone, originalComment)

    /** The JSON the engine actually sends (same Json config as the HTTP client). */
    private fun json(request: DnsRecordRequest): JsonObject =
        cfJson.parseToJsonElement(cfJson.encodeToString(DnsRecordRequest.serializer(), request)).jsonObject

    @Test
    fun `a record sends content and proxied, not priority or data`() {
        val body = json(build(DnsRecordForm(DnsRecordType.A, name = "www", content = " 192.0.2.1 ", proxied = true, ttl = 300)))
        assertEquals(setOf("type", "name", "ttl", "comment", "content", "proxied"), body.keys)
        assertEquals("www.example.com", body["name"]!!.jsonPrimitive.content)
        assertEquals("192.0.2.1", body["content"]!!.jsonPrimitive.content)
        assertEquals("A", body["type"]!!.jsonPrimitive.content)
        // Proxied records are always Auto TTL
        assertEquals(1, body["ttl"]!!.jsonPrimitive.int)
    }

    @Test
    fun `dns only a record keeps its ttl`() {
        val request = build(DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1", proxied = false, ttl = 300))
        assertEquals(300, request.ttl)
        assertEquals(false, request.proxied)
        assertEquals("example.com", request.name)
    }

    @Test
    fun `cname and aaaa may be proxied`() {
        assertEquals(true, build(DnsRecordForm(DnsRecordType.CNAME, name = "blog", content = "example.net.", proxied = true)).proxied)
        assertEquals(true, build(DnsRecordForm(DnsRecordType.AAAA, name = "v6", content = "2001:db8::1", proxied = true)).proxied)
        assertEquals("example.net", build(DnsRecordForm(DnsRecordType.CNAME, name = "blog", content = "example.net.")).content)
    }

    @Test
    fun `mx sends priority and never proxied`() {
        val body = json(build(DnsRecordForm(DnsRecordType.MX, name = "@", content = "mx.example.net", priority = "20", proxied = true, ttl = 3600)))
        assertEquals(setOf("type", "name", "ttl", "comment", "content", "priority"), body.keys)
        assertEquals(20, body["priority"]!!.jsonPrimitive.int)
        assertEquals(3600, body["ttl"]!!.jsonPrimitive.int)
    }

    @Test
    fun `txt and ns send only content`() {
        val txt = json(build(DnsRecordForm(DnsRecordType.TXT, name = "_dmarc", content = "\"v=DMARC1; p=none\"", proxied = true)))
        assertEquals(setOf("type", "name", "ttl", "comment", "content"), txt.keys)
        assertEquals("_dmarc.example.com", txt["name"]!!.jsonPrimitive.content)
        assertEquals("\"v=DMARC1; p=none\"", txt["content"]!!.jsonPrimitive.content)
        val ns = json(build(DnsRecordForm(DnsRecordType.NS, name = "sub", content = "ns1.example.net")))
        assertEquals(setOf("type", "name", "ttl", "comment", "content"), ns.keys)
    }

    @Test
    fun `caa sends data with flags tag and value`() {
        val request = build(DnsRecordForm(DnsRecordType.CAA, name = "@", caaFlags = "128", caaTag = "issuewild", caaValue = " letsencrypt.org "))
        assertEquals(DnsRecordData(flags = 128, tag = "issuewild", value = "letsencrypt.org"), request.data)
        val body = json(request)
        assertEquals(setOf("type", "name", "ttl", "comment", "data"), body.keys)
        assertEquals(setOf("flags", "tag", "value"), body["data"]!!.jsonObject.keys)
    }

    @Test
    fun `srv builds the name from service and proto and sends data`() {
        val form = DnsRecordForm(
            DnsRecordType.SRV, name = "", srvService = "sip", srvProto = "_tcp",
            srvPriority = "10", srvWeight = "5", srvPort = "5060", srvTarget = "sip.example.com.",
        )
        val request = build(form)
        assertEquals("_sip._tcp.example.com", request.name)
        assertEquals(DnsRecordData(priority = 10, weight = 5, port = 5060, target = "sip.example.com"), request.data)
        val body = json(request)
        assertEquals(setOf("type", "name", "ttl", "comment", "data"), body.keys)
        assertEquals(setOf("priority", "weight", "port", "target"), body["data"]!!.jsonObject.keys)

        assertEquals("_sip._tcp.voice.example.com", build(form.copy(name = "voice")).name)
    }

    @Test
    fun `srv form round trips from an existing record`() {
        val record = DnsRecord(
            id = "r", name = "_sip._tcp.voice.example.com", type = DnsRecordType.SRV,
            content = "5 5060 sip.example.com", ttl = 1, priority = 10,
            data = DnsRecordData(priority = 10, weight = 5, port = 5060, target = "sip.example.com"),
        )
        val form = DnsRecordForm.fromRecord(record)
        assertEquals("_sip", form.srvService)
        assertEquals("_tcp", form.srvProto)
        assertEquals("voice.example.com", form.name)
        assertEquals(record.name, build(form).name)
    }

    @Test
    fun `comment is sent as null when blank so it clears on cloudflare`() {
        val body = json(build(DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1", comment = "  ")))
        assertTrue(body.containsKey("comment"))
        assertEquals("null", body["comment"].toString())
        assertEquals("office vpn", build(DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1", comment = " office vpn ")).comment)
    }

    @Test
    fun `lock marker is preserved when the original comment had it`() {
        val original = LockMarker.add("old note")
        val request = build(DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1", comment = "new note"), original)
        assertTrue(LockMarker.isLocked(request.comment))
        assertEquals("new note", LockMarker.remove(request.comment))
        assertTrue(request.comment!!.length <= 100)

        val empty = build(DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1", comment = ""), original)
        assertEquals(LockMarker.TOKEN, empty.comment)
    }

    @Test
    fun `users cannot type the lock marker in by hand`() {
        val request = build(DnsRecordForm(DnsRecordType.A, name = "@", content = "192.0.2.1", comment = "${LockMarker.TOKEN} sneaky"))
        assertFalse(LockMarker.isLocked(request.comment))
    }

    @Test
    fun `name qualification`() {
        assertEquals("example.com", DnsRecordRequestBuilder.qualifyName("@", zone))
        assertEquals("example.com", DnsRecordRequestBuilder.qualifyName("", zone))
        assertEquals("www.example.com", DnsRecordRequestBuilder.qualifyName("www", zone))
        assertEquals("www.example.com", DnsRecordRequestBuilder.qualifyName("www.example.com.", zone))
        assertEquals("WWW.Example.com", DnsRecordRequestBuilder.qualifyName("WWW.Example.com", zone))
        assertEquals("notexample.com.example.com", DnsRecordRequestBuilder.qualifyName("notexample.com", zone))
        assertEquals("www", DnsRecordRequestBuilder.qualifyName("www", null))
    }

    @Test
    fun `comment only patch body`() {
        val body = cfJson.encodeToString(DnsCommentUpdate.serializer(), DnsCommentUpdate(null))
        assertEquals("""{"comment":null}""", body)
    }
}
