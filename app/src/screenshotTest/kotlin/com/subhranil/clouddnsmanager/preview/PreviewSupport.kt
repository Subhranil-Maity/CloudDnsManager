package com.subhranil.clouddnsmanager.preview

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.subhranil.clouddnsmanager.dns.DnsRecordItem
import com.subhranil.clouddnsmanager.email.aliases.AliasRow
import com.subhranil.clouddnsmanager.email.domain.toAliasDisplay
import com.subhranil.clouddnsmanager.email.model.EmailActivityEvent
import com.subhranil.clouddnsmanager.email.model.EmailDestinationAddress
import com.subhranil.clouddnsmanager.email.model.EmailRoutingRule
import com.subhranil.clouddnsmanager.email.model.EmailRuleAction
import com.subhranil.clouddnsmanager.email.model.EmailRuleMatcher
import com.subhranil.clouddnsmanager.localstore.lock.LockMarker
import com.subhranil.clouddnsmanager.localstore.lock.LockStatus
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsRecordData
import com.subhranil.clouddnsmanager.models.dns.DnsRecordMeta
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType
import com.subhranil.clouddnsmanager.models.zone.Zone
import com.subhranil.clouddnsmanager.models.zone.ZoneAccount
import com.subhranil.clouddnsmanager.models.zone.ZonePlan
import com.subhranil.clouddnsmanager.models.zone.ZoneStatus
import com.subhranil.clouddnsmanager.models.zone.ZoneType
import com.subhranil.clouddnsmanager.ui.theme.CloudDnsManagerTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Shared helpers for the Compose previews in this source set, which are also rendered to PNGs
 * for the README (./gradlew :app:updateDebugScreenshotTest).
 * Everything here is made-up sample data: reserved example domains and addresses only.
 */

/** Phone-sized preview used for every screen preview, rendered in both light and dark mode. */
@Preview(name = "Light", showBackground = true, widthDp = 411, heightDp = 891)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF141218,
    widthDp = 411,
    heightDp = 891,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class ScreenPreview

/**
 * App theme with a fixed (non-dynamic) colour scheme so screenshots look the same everywhere.
 * Light or dark follows the preview's night mode (see [ScreenPreview]).
 */
@Composable
fun PreviewTheme(content: @Composable () -> Unit) {
    CloudDnsManagerTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false, content = content)
}

object SampleData {
    val account = ZoneAccount(id = "acc-001", name = "Alex's Account")

    const val ZONE = "example.com"

    val zones = listOf(
        zone("zone-1", "example.com", ZoneStatus.ACTIVE, "Free Website"),
        zone("zone-2", "acme-labs.dev", ZoneStatus.ACTIVE, "Pro Website"),
        zone("zone-3", "myblog.io", ZoneStatus.ACTIVE, "Free Website"),
        zone("zone-4", "shop.example.org", ZoneStatus.PENDING, "Free Website"),
    )

    private fun zone(id: String, name: String, status: ZoneStatus, plan: String) = Zone(
        id = id,
        name = name,
        status = status,
        paused = false,
        type = ZoneType.FULL,
        nameServers = listOf("ada.ns.cloudflare.com", "bob.ns.cloudflare.com"),
        account = account,
        plan = ZonePlan(name = plan),
        createdOn = "2025-03-14T10:00:00Z",
        modifiedOn = "2026-09-20T08:30:00Z",
    )

    // ── DNS ─────────────────────────────────────────────────────────────────

    private fun record(
        id: String,
        type: DnsRecordType,
        name: String,
        content: String,
        proxied: Boolean = false,
        ttl: Int = 1,
        priority: Int? = null,
        comment: String? = null,
        meta: DnsRecordMeta = DnsRecordMeta(),
        locked: Boolean = false,
        data: DnsRecordData? = null,
    ) = DnsRecord(
        id = id,
        zoneId = "zone-1",
        zoneName = ZONE,
        name = name,
        type = type,
        content = content,
        proxiable = type in setOf(DnsRecordType.A, DnsRecordType.AAAA, DnsRecordType.CNAME),
        proxied = proxied,
        ttl = ttl,
        locked = locked,
        priority = priority,
        data = data,
        comment = comment,
        meta = meta,
        createdOn = "2025-03-14T10:00:00Z",
        modifiedOn = "2026-09-20T08:30:00Z",
    )

    private val emailRoutingMeta = DnsRecordMeta(readOnly = true, emailRouting = true)
    private const val MANAGED = "Managed by Cloudflare Email Routing"

    val apexRecord = record(
        "r1", DnsRecordType.A, ZONE, "203.0.113.10", proxied = true,
        comment = LockMarker.add("Main website"),
    )

    val dnsItems = listOf(
        DnsRecordItem(apexRecord, LockStatus.UserLocked, note = "Origin server for the main site. Keep until the migration is done."),
        DnsRecordItem(record("r2", DnsRecordType.AAAA, ZONE, "2001:db8::10", proxied = true), LockStatus.Unlocked, null),
        DnsRecordItem(record("r3", DnsRecordType.CNAME, "www.$ZONE", ZONE, proxied = true), LockStatus.Unlocked, null),
        DnsRecordItem(record("r4", DnsRecordType.A, "api.$ZONE", "198.51.100.24", ttl = 300, comment = "Backend API"), LockStatus.Unlocked, "Rotates with the load balancer"),
        DnsRecordItem(record("r5", DnsRecordType.CNAME, "blog.$ZONE", "example-blog.pages.dev", proxied = true), LockStatus.Unlocked, null),
        DnsRecordItem(record("r6", DnsRecordType.MX, ZONE, "route1.mx.cloudflare.net", priority = 13, meta = emailRoutingMeta, locked = true), LockStatus.Managed(MANAGED), null),
        DnsRecordItem(record("r7", DnsRecordType.MX, ZONE, "route2.mx.cloudflare.net", priority = 86, meta = emailRoutingMeta, locked = true), LockStatus.Managed(MANAGED), null),
        DnsRecordItem(record("r8", DnsRecordType.TXT, ZONE, "\"v=spf1 include:_spf.mx.cloudflare.net ~all\"", meta = emailRoutingMeta, locked = true), LockStatus.Managed(MANAGED), null),
        DnsRecordItem(
            record("r9", DnsRecordType.CAA, ZONE, "0 issue \"letsencrypt.org\"", data = DnsRecordData(flags = 0, tag = "issue", value = "letsencrypt.org")),
            LockStatus.Unlocked, null,
        ),
        DnsRecordItem(
            record("r10", DnsRecordType.SRV, "_sip._tcp.$ZONE", "10 5 5060 sip.$ZONE", ttl = 3600,
                data = DnsRecordData(priority = 10, weight = 5, port = 5060, target = "sip.$ZONE")),
            LockStatus.Unlocked, null,
        ),
    )

    // ── Email Routing ───────────────────────────────────────────────────────

    private fun rule(id: String, alias: String, forwardTo: String, name: String? = null, enabled: Boolean = true) = EmailRoutingRule(
        id = id,
        name = name,
        enabled = enabled,
        matchers = listOf(EmailRuleMatcher(type = "literal", field = "to", value = "$alias@$ZONE")),
        actions = listOf(EmailRuleAction(type = "forward", value = listOf(forwardTo))),
    )

    val rules = listOf(
        rule("a1", "hello", "alex@example.net", name = "Public contact"),
        rule("a2", "quiet-river-4821", "alex@example.net", name = "Online shopping"),
        rule("a3", "amber-falcon-3907", "alex@example.net", name = "Newsletters"),
        rule("a4", "billing", "finance@example.org", name = "Invoices"),
        rule("a5", "misty-harbor-1266", "alex@example.net", name = "Old forum sign-up", enabled = false),
        rule("a6", "support", "team@example.org"),
    )

    val aliasRows = rules.map { AliasRow(it, it.toAliasDisplay()) }

    val catchAll = EmailRoutingRule(
        id = "catch-all",
        name = "Catch-all",
        enabled = false,
        matchers = listOf(EmailRuleMatcher(type = "all")),
        actions = listOf(EmailRuleAction(type = "drop")),
    ).toAliasDisplay()

    val addresses = listOf(
        EmailDestinationAddress(id = "d1", email = "alex@example.net", created = "2025-03-14T10:00:00Z", verified = "2025-03-14T10:05:00Z"),
        EmailDestinationAddress(id = "d2", email = "finance@example.org", created = "2025-06-02T09:00:00Z", verified = "2025-06-02T09:12:00Z"),
        EmailDestinationAddress(id = "d3", email = "team@example.org", created = "2026-01-20T16:40:00Z", verified = "2026-01-20T16:44:00Z"),
        EmailDestinationAddress(id = "d4", email = "alex.backup@example.com", created = "2026-09-27T18:00:00Z", verified = null),
    )

    val aliasesByDestination = mapOf(
        "alex@example.net" to listOf("hello@$ZONE", "quiet-river-4821@$ZONE", "amber-falcon-3907@$ZONE", "misty-harbor-1266@$ZONE"),
        "finance@example.org" to listOf("billing@$ZONE"),
        "team@example.org" to listOf("support@$ZONE"),
    )

    private val now: Instant = Instant.parse("2026-09-28T09:30:00Z")
    private fun ago(minutes: Long) = now.minus(minutes, ChronoUnit.MINUTES).toString()

    val activity = listOf(
        event("e1", ago(12), "orders@shop.example", "quiet-river-4821@$ZONE", "Your order #10234 has shipped", "delivered"),
        event("e2", ago(95), "news@weekly.example", "amber-falcon-3907@$ZONE", "This week in web performance", "delivered"),
        event("e3", ago(240), "jane@example.org", "hello@$ZONE", "Question about your blog post", "delivered"),
        event("e4", ago(410), "no-reply@billing.example", "billing@$ZONE", "Invoice INV-2026-0917 is available", "delivered"),
        event("e5", ago(1_080), "promo@deals.example", "misty-harbor-1266@$ZONE", "Last chance: 70% off", "rejected"),
        event("e6", ago(1_500), "unknown@spam.example", "random@$ZONE", "You have won!", "dropped"),
    )

    val aliasActivity = activity.filter { it.to == "quiet-river-4821@$ZONE" } + listOf(
        event("e7", ago(2_900), "orders@shop.example", "quiet-river-4821@$ZONE", "Order confirmation #10234", "delivered"),
        event("e8", ago(7_300), "accounts@shop.example", "quiet-river-4821@$ZONE", "Welcome to Example Shop", "delivered"),
    )

    private fun event(id: String, at: String, from: String, to: String, subject: String, status: String) = EmailActivityEvent(
        id = id,
        datetime = at,
        from = from,
        to = to,
        subject = subject,
        status = status,
        action = if (status == "dropped") "drop" else "forward",
        spf = "pass",
        dkim = "pass",
        dmarc = "pass",
    )
}
