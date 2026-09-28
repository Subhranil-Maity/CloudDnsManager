package com.subhranil.clouddnsmanager.dns.edit

import com.subhranil.clouddnsmanager.dns.record.DNS_COMMENT_MAX_LENGTH
import com.subhranil.clouddnsmanager.dns.record.supportsProxy
import com.subhranil.clouddnsmanager.localstore.lock.LockMarker
import com.subhranil.clouddnsmanager.models.dns.DnsRecordData
import com.subhranil.clouddnsmanager.models.dns.DnsRecordRequest
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType

/**
 * Turns a validated [DnsRecordForm] into the JSON body Cloudflare expects for its type.
 * Pure so the per-type field rules can be unit tested.
 */
object DnsRecordRequestBuilder {

    /**
     * @param zoneName used to expand "@" and relative names into the complete name the API
     *   documents; when unknown the name is sent as typed (Cloudflare also accepts that).
     * @param originalComment the record's comment on Cloudflare before editing. If it carries
     *   the lock marker, the marker is kept so saving never silently drops a lock.
     */
    fun build(form: DnsRecordForm, zoneName: String?, originalComment: String? = null): DnsRecordRequest {
        val type = form.type
        val proxied = if (type.supportsProxy()) form.proxied else null
        // Proxied records always use Auto TTL on Cloudflare
        val ttl = if (proxied == true) 1 else form.ttl

        val userComment = LockMarker.remove(form.comment.trim())
        val comment = when {
            LockMarker.isLocked(originalComment) -> LockMarker.add(userComment, DNS_COMMENT_MAX_LENGTH)
            userComment.isEmpty() -> null
            else -> userComment
        }

        return when (type) {
            DnsRecordType.CAA -> DnsRecordRequest(
                type = type,
                name = qualifyName(form.name, zoneName),
                ttl = ttl,
                comment = comment,
                data = DnsRecordData(
                    flags = form.caaFlags.trim().toInt(),
                    tag = form.caaTag,
                    value = form.caaValue.trim(),
                ),
            )
            DnsRecordType.SRV -> {
                val service = "_" + form.srvService.trim().removePrefix("_")
                val proto = "_" + form.srvProto.trim().removePrefix("_")
                val base = form.name.trim().removeSuffix(".")
                val relative = if (base.isEmpty() || base == "@") "$service.$proto" else "$service.$proto.$base"
                DnsRecordRequest(
                    type = type,
                    name = qualifyName(relative, zoneName),
                    ttl = ttl,
                    comment = comment,
                    data = DnsRecordData(
                        priority = form.srvPriority.trim().toInt(),
                        weight = form.srvWeight.trim().toInt(),
                        port = form.srvPort.trim().toInt(),
                        target = form.srvTarget.trim().removeSuffix(".").ifEmpty { "." },
                    ),
                )
            }
            else -> DnsRecordRequest(
                type = type,
                name = qualifyName(form.name, zoneName),
                ttl = ttl,
                comment = comment,
                content = form.content.trim().let { if (type == DnsRecordType.TXT) it else it.removeSuffix(".") },
                proxied = proxied,
                priority = if (type == DnsRecordType.MX) form.priority.trim().toInt() else null,
            )
        }
    }

    /**
     * "@" or blank → the zone apex; "www" → "www.example.com"; names already ending in the
     * zone are kept. Trailing dots are dropped.
     */
    fun qualifyName(input: String, zoneName: String?): String {
        val name = input.trim().removeSuffix(".")
        if (zoneName.isNullOrBlank()) return name.ifEmpty { "@" }
        val zone = zoneName.removeSuffix(".")
        return when {
            name.isEmpty() || name == "@" -> zone
            name.equals(zone, ignoreCase = true) || name.endsWith(".$zone", ignoreCase = true) -> name
            else -> "$name.$zone"
        }
    }
}
