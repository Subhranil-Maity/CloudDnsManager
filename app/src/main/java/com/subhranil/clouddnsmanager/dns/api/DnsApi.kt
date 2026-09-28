package com.subhranil.clouddnsmanager.dns.api

import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.http.CloudflareHttpClient

/**
 * DNS write operations (create / update / delete records).
 *
 * Built on the shared engine's post/put/patch/delete helpers so the DNS feature never has
 * to edit CloudflareClient. Reads still use CloudflareClient.listDnsRecords/allDnsRecords.
 *
 * Get one from an authenticated client: `sessionManager.clientOrNull()?.dnsApi()`.
 */
internal class DnsApi(private val engine: CloudflareHttpClient) {
    // Endpoints are added by the DNS feature work, e.g.
    // POST   /zones/{zoneId}/dns_records
    // PATCH  /zones/{zoneId}/dns_records/{recordId}
    // DELETE /zones/{zoneId}/dns_records/{recordId}
}

internal fun CloudflareClient.dnsApi(): DnsApi = DnsApi(engine)
