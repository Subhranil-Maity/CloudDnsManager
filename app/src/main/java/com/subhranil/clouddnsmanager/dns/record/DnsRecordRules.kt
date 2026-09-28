package com.subhranil.clouddnsmanager.dns.record

import com.subhranil.clouddnsmanager.localstore.lock.LockMarker
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType

/** Cloudflare's comment limit on the free plan; paid plans allow more, so this is the safe bound. */
const val DNS_COMMENT_MAX_LENGTH = 100

/** Record types this app can create and edit. Other types open read-only. */
val EDITABLE_DNS_TYPES: List<DnsRecordType> = listOf(
    DnsRecordType.A,
    DnsRecordType.AAAA,
    DnsRecordType.CNAME,
    DnsRecordType.TXT,
    DnsRecordType.MX,
    DnsRecordType.NS,
    DnsRecordType.CAA,
    DnsRecordType.SRV,
)

fun DnsRecordType.isEditableInApp(): Boolean = this in EDITABLE_DNS_TYPES

/** Only these types can be proxied through Cloudflare (orange cloud). */
fun DnsRecordType.supportsProxy(): Boolean =
    this == DnsRecordType.A || this == DnsRecordType.AAAA || this == DnsRecordType.CNAME

/**
 * Why Cloudflare itself protects [record], or null when the user may change it.
 *
 * A non-null result feeds `ItemLockManager.status(userLocked, managedReason)`, which turns it
 * into `LockStatus.Managed`: such records can't be edited, deleted or locked in the app.
 * The most specific product wins, so an Email Routing record that is also `read_only`
 * still says "Email Routing".
 */
fun managedReason(record: DnsRecord): String? {
    val meta = record.meta
    return when {
        meta.emailRouting -> "Managed by Cloudflare Email Routing"
        meta.managedByArgoTunnel -> "Managed by a Cloudflare Tunnel"
        meta.managedByApps -> "Managed by a Cloudflare app"
        meta.readOnly -> "Read-only record managed by Cloudflare"
        record.locked -> "Locked by Cloudflare"
        else -> null
    }
}

/** The comment as the user should see it: the app's lock marker stripped, null when empty. */
fun DnsRecord.displayComment(): String? = LockMarker.remove(comment).ifEmpty { null }
