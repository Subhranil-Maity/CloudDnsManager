package com.subhranil.clouddnsmanager.models.dns

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Read-only metadata Cloudflare attaches to a record.
 *
 * The current API reference only documents the glue / shadowing fields, but Cloudflare still
 * returns the older product flags (`read_only`, `email_routing`, `managed_by_*`) on records
 * other products own, so every field is optional and defaults to "not set".
 */
@Serializable
data class DnsRecordMeta(
    @SerialName("auto_added") val autoAdded: Boolean = false,
    @SerialName("read_only") val readOnly: Boolean = false,
    @SerialName("email_routing") val emailRouting: Boolean = false,
    @SerialName("managed_by_apps") val managedByApps: Boolean = false,
    @SerialName("managed_by_argo_tunnel") val managedByArgoTunnel: Boolean = false,
    val source: String? = null,
)
