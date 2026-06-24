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
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("modified_on") val modifiedOn: String? = null,
)