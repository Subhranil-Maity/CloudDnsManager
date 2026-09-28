package com.subhranil.clouddnsmanager.dns.validation

import com.subhranil.clouddnsmanager.dns.edit.CAA_TAGS
import com.subhranil.clouddnsmanager.dns.edit.DnsField
import com.subhranil.clouddnsmanager.dns.edit.DnsRecordForm
import com.subhranil.clouddnsmanager.dns.record.DNS_COMMENT_MAX_LENGTH
import com.subhranil.clouddnsmanager.dns.record.isEditableInApp
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType

/**
 * Pure, Android-free checks for the record editor. Returns one message per invalid field;
 * an empty map means the form can be sent to Cloudflare.
 *
 * These catch typos early. Cloudflare still has the final say (e.g. plan-specific limits),
 * and its errors are shown inline by the editor.
 */
object DnsRecordValidator {

    const val TTL_AUTO = 1
    /** Enterprise zones allow 30; other plans start at 60 and Cloudflare reports that itself. */
    const val TTL_MIN = 30
    const val TTL_MAX = 86400
    /** Cloudflare's limit for TXT content. */
    const val TXT_MAX_LENGTH = 2048

    fun validate(form: DnsRecordForm): Map<DnsField, String> = buildMap {
        if (!form.type.isEditableInApp()) {
            put(DnsField.CONTENT, "${form.type.name} records can't be edited in this app.")
            return@buildMap
        }

        // Name: SRV builds the name from service/proto, so a blank base means the zone apex
        val nameError = if (form.type == DnsRecordType.SRV) {
            if (form.name.isBlank()) null else recordNameError(form.name)
        } else {
            if (form.name.isBlank()) "Enter a name, or @ for the zone apex." else recordNameError(form.name)
        }
        nameError?.let { put(DnsField.NAME, it) }

        if (form.ttl != TTL_AUTO && form.ttl !in TTL_MIN..TTL_MAX) {
            put(DnsField.TTL, "TTL must be Auto or between $TTL_MIN and $TTL_MAX seconds.")
        }

        if (form.comment.length > DNS_COMMENT_MAX_LENGTH) {
            put(DnsField.COMMENT, "Comments can be at most $DNS_COMMENT_MAX_LENGTH characters.")
        }

        when (form.type) {
            DnsRecordType.A -> when {
                form.content.isBlank() -> put(DnsField.CONTENT, "Enter an IPv4 address.")
                !isIpv4(form.content.trim()) -> put(DnsField.CONTENT, "Not a valid IPv4 address (e.g. 192.0.2.1).")
            }
            DnsRecordType.AAAA -> when {
                form.content.isBlank() -> put(DnsField.CONTENT, "Enter an IPv6 address.")
                !isIpv6(form.content.trim()) -> put(DnsField.CONTENT, "Not a valid IPv6 address (e.g. 2001:db8::1).")
            }
            DnsRecordType.CNAME, DnsRecordType.NS ->
                hostnameError(form.content, "Enter the target hostname.")?.let { put(DnsField.CONTENT, it) }
            DnsRecordType.MX -> {
                hostnameError(form.content, "Enter the mail server hostname.")?.let { put(DnsField.CONTENT, it) }
                rangeError(form.priority, 0, 65535, "Priority")?.let { put(DnsField.PRIORITY, it) }
            }
            DnsRecordType.TXT -> when {
                form.content.isBlank() -> put(DnsField.CONTENT, "Enter the text content.")
                form.content.length > TXT_MAX_LENGTH ->
                    put(DnsField.CONTENT, "TXT content can be at most $TXT_MAX_LENGTH characters.")
            }
            DnsRecordType.CAA -> {
                rangeError(form.caaFlags, 0, 255, "Flags")?.let { put(DnsField.CAA_FLAGS, it) }
                if (form.caaTag !in CAA_TAGS) put(DnsField.CAA_TAG, "Choose one of ${CAA_TAGS.joinToString()}.")
                val value = form.caaValue.trim()
                when {
                    value.isEmpty() -> put(DnsField.CAA_VALUE, "Enter a value (e.g. letsencrypt.org).")
                    form.caaTag == "iodef" && !(value.startsWith("mailto:") ||
                        value.startsWith("https://") || value.startsWith("http://")) ->
                        put(DnsField.CAA_VALUE, "iodef needs a mailto: or https:// URL.")
                }
            }
            DnsRecordType.SRV -> {
                val service = form.srvService.trim().removePrefix("_")
                when {
                    service.isEmpty() -> put(DnsField.SRV_SERVICE, "Enter the service (e.g. _sip).")
                    !LABEL_REGEX.matches(service) -> put(DnsField.SRV_SERVICE, "Use letters, digits and hyphens only.")
                }
                val proto = form.srvProto.trim().removePrefix("_")
                when {
                    proto.isEmpty() -> put(DnsField.SRV_PROTO, "Choose a protocol.")
                    !LABEL_REGEX.matches(proto) -> put(DnsField.SRV_PROTO, "Use letters, digits and hyphens only.")
                }
                rangeError(form.srvPriority, 0, 65535, "Priority")?.let { put(DnsField.SRV_PRIORITY, it) }
                rangeError(form.srvWeight, 0, 65535, "Weight")?.let { put(DnsField.SRV_WEIGHT, it) }
                rangeError(form.srvPort, 0, 65535, "Port")?.let { put(DnsField.SRV_PORT, it) }
                // "." is the standard way to say "service not available here"
                if (form.srvTarget.trim() != ".") {
                    hostnameError(form.srvTarget, "Enter the target hostname.")?.let { put(DnsField.SRV_TARGET, it) }
                }
            }
            else -> Unit
        }
    }

    // ── Building blocks (public so they can be tested on their own) ────────────

    fun isIpv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.all { it.isDigit() } &&
                (part.length == 1 || part[0] != '0') && part.toInt() <= 255
        }
    }

    fun isIpv6(value: String): Boolean {
        if (value.isEmpty() || value.contains('%')) return false
        var groupsNeeded = 8
        var address = value
        // An embedded IPv4 tail (e.g. ::ffff:192.0.2.1) counts as two groups
        val lastColon = address.lastIndexOf(':')
        if (lastColon >= 0 && address.substring(lastColon + 1).contains('.')) {
            if (!isIpv4(address.substring(lastColon + 1))) return false
            address = address.substring(0, lastColon + 1) + "0"
            groupsNeeded = 7
        }
        val doubleColons = address.windowed(2).count { it == "::" }
        if (doubleColons > 1 || address.contains(":::")) return false
        fun validGroups(part: String): List<String>? {
            if (part.isEmpty()) return emptyList()
            val groups = part.split(':')
            return if (groups.all { it.length in 1..4 && it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }) groups else null
        }
        return if (doubleColons == 1) {
            val head = validGroups(address.substringBefore("::")) ?: return false
            val tail = validGroups(address.substringAfter("::")) ?: return false
            head.size + tail.size < groupsNeeded
        } else {
            val groups = validGroups(address) ?: return false
            groups.size == groupsNeeded
        }
    }

    /**
     * A hostname a record points to (CNAME/MX/NS/SRV target): at least two labels,
     * letters/digits/hyphens/underscores, optional trailing dot, Punycode for IDNs.
     */
    fun isHostname(value: String): Boolean {
        val host = value.removeSuffix(".")
        if (host.isEmpty() || host.length > 253) return false
        val labels = host.split('.')
        return labels.size >= 2 && labels.all { LABEL_REGEX.matches(it) }
    }

    /** A record name: "@", a relative label chain or a full name; "*" allowed as the first label. */
    fun isRecordName(value: String): Boolean {
        val name = value.trim().removeSuffix(".")
        if (name == "@") return true
        if (name.isEmpty() || name.length > 253) return false
        val labels = name.split('.')
        return labels.withIndex().all { (index, label) ->
            (index == 0 && label == "*") || LABEL_REGEX.matches(label)
        }
    }

    private fun recordNameError(value: String): String? = when {
        value.any { it.code > 127 } -> "Use Punycode (xn--...) for international names."
        !isRecordName(value) -> "Not a valid name. Use letters, digits, hyphens and dots, or @."
        else -> null
    }

    private fun hostnameError(value: String, emptyMessage: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> emptyMessage
            trimmed.any { it.code > 127 } -> "Use Punycode (xn--...) for international names."
            !isHostname(trimmed) -> "Not a valid hostname (e.g. mail.example.com)."
            else -> null
        }
    }

    private fun rangeError(value: String, min: Int, max: Int, label: String): String? {
        val number = value.trim().toIntOrNull()
        return when {
            value.isBlank() -> "$label is required."
            number == null || number !in min..max -> "$label must be a number from $min to $max."
            else -> null
        }
    }

    /** One DNS label: 1-63 chars, no leading/trailing hyphen. Underscores allowed (_dmarc, _domainkey). */
    private val LABEL_REGEX = Regex("^(?!-)[A-Za-z0-9_-]{1,63}(?<!-)$")
}
