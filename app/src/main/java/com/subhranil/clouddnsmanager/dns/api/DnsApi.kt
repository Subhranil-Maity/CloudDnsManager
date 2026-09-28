package com.subhranil.clouddnsmanager.dns.api

import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.http.CloudflareHttpClient
import com.subhranil.clouddnsmanager.models.dns.DeletedDnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsCommentUpdate
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsRecordRequest

/**
 * DNS write operations (create / update / delete records).
 *
 * Built on the shared engine's post/put/patch/delete helpers so the DNS feature never has
 * to edit CloudflareClient. Reads still use CloudflareClient.listDnsRecords/allDnsRecords.
 * Every call here needs the token permission "DNS: Edit".
 *
 * Get one from an authenticated client: `sessionManager.clientOrNull()?.dnsApi()`.
 * Only call these from a ViewModel, after `authGate.authorize(...)` (architecture.md §8a).
 */
internal class DnsApi(private val engine: CloudflareHttpClient) {

    /** POST /zones/{zoneId}/dns_records */
    suspend fun createRecord(zoneId: String, request: DnsRecordRequest): DnsRecord =
        engine.post("/zones/$zoneId/dns_records", request)

    /** PATCH /zones/{zoneId}/dns_records/{recordId}: fields left out of [request] are kept. */
    suspend fun updateRecord(zoneId: String, recordId: String, request: DnsRecordRequest): DnsRecord =
        engine.patch("/zones/$zoneId/dns_records/$recordId", request)

    /** DELETE /zones/{zoneId}/dns_records/{recordId} */
    suspend fun deleteRecord(zoneId: String, recordId: String): DeletedDnsRecord =
        engine.delete("/zones/$zoneId/dns_records/$recordId")

    /** PATCH only the record's comment; null clears it. */
    suspend fun updateComment(zoneId: String, recordId: String, comment: String?): DnsRecord =
        engine.patch("/zones/$zoneId/dns_records/$recordId", DnsCommentUpdate(comment))
}

internal fun CloudflareClient.dnsApi(): DnsApi = DnsApi(engine)
