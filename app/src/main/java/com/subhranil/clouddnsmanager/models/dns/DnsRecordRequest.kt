package com.subhranil.clouddnsmanager.models.dns

import kotlinx.serialization.Serializable

/**
 * Body for creating (POST) or editing (PATCH) a DNS record.
 *
 * Optional fields default to null and are left out of the JSON, so each record type only
 * sends what it needs: `content` for simple types, `data` for SRV / CAA, `priority` for MX,
 * `proxied` only for A / AAAA / CNAME. `comment` has no default, so it is always sent:
 * null clears the comment on Cloudflare. Tags are never sent, so a PATCH keeps them.
 *
 * Build it with `DnsRecordRequestBuilder` rather than by hand.
 */
@Serializable
data class DnsRecordRequest(
    val type: DnsRecordType,
    /** Complete name including the zone, as the API expects. */
    val name: String,
    /** 1 = Auto. */
    val ttl: Int,
    val comment: String?,
    val content: String? = null,
    val proxied: Boolean? = null,
    val priority: Int? = null,
    val data: DnsRecordData? = null,
)

/** PATCH body that only changes a record's comment (used to sync the lock marker). */
@Serializable
data class DnsCommentUpdate(val comment: String?)

/** What Cloudflare returns from DELETE /dns_records/{id}. */
@Serializable
data class DeletedDnsRecord(val id: String)
