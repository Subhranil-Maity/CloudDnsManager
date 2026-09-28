package com.subhranil.clouddnsmanager.models.dns

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DnsRecord(
    val id: String,
    @SerialName("zone_id") val zoneId: String? = null,
    @SerialName("zone_name") val zoneName: String? = null,
    val name: String,
    val type: DnsRecordType,
    val content: String,
    val proxiable: Boolean = false,
    val proxied: Boolean = false,
    val ttl: Int,
    val locked: Boolean = false,
    val priority: Int? = null,        // MX / SRV / URI
    val data: DnsRecordData? = null,  // SRV / LOC / CAA / etc.
    /** Cloudflare's own comment field (max 100 chars on the free plan). May carry the lock marker. */
    val comment: String? = null,
    val tags: List<String> = emptyList(),
    /** Read-only flags Cloudflare sets; see [DnsRecordMeta]. */
    val meta: DnsRecordMeta = DnsRecordMeta(),
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("modified_on") val modifiedOn: String? = null,
)
