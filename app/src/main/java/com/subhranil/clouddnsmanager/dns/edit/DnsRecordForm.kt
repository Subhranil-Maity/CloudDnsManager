package com.subhranil.clouddnsmanager.dns.edit

import com.subhranil.clouddnsmanager.dns.record.displayComment
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType

/** Every input on the editor that can carry its own validation error. */
enum class DnsField {
    NAME, CONTENT, TTL, PRIORITY, COMMENT,
    CAA_FLAGS, CAA_TAG, CAA_VALUE,
    SRV_SERVICE, SRV_PROTO, SRV_PRIORITY, SRV_WEIGHT, SRV_PORT, SRV_TARGET,
}

/** CAA property tags Cloudflare accepts. */
val CAA_TAGS = listOf("issue", "issuewild", "iodef")

/** SRV protocols offered in the editor. */
val SRV_PROTOCOLS = listOf("_tcp", "_udp", "_tls")

/** TTL choices offered in the editor, in seconds. 1 = Auto. */
val COMMON_TTLS = listOf(1, 60, 120, 300, 600, 900, 1800, 3600, 7200, 18000, 43200, 86400)

/**
 * Raw editor input. Numbers are kept as text so partially typed values survive until
 * [com.subhranil.clouddnsmanager.dns.validation.DnsRecordValidator] checks them.
 *
 * [name] is what the user typed: "@", a label relative to the zone ("www") or a full name.
 * For SRV it is the part after `_service._proto` (blank or "@" = the zone apex).
 * [comment] never contains the lock marker; the request builder re-adds it when needed.
 */
data class DnsRecordForm(
    val type: DnsRecordType = DnsRecordType.A,
    val name: String = "",
    val content: String = "",
    val ttl: Int = 1,
    val proxied: Boolean = false,
    val priority: String = "10",
    val comment: String = "",
    val caaFlags: String = "0",
    val caaTag: String = CAA_TAGS.first(),
    val caaValue: String = "",
    val srvService: String = "",
    val srvProto: String = SRV_PROTOCOLS.first(),
    val srvPriority: String = "10",
    val srvWeight: String = "5",
    val srvPort: String = "",
    val srvTarget: String = "",
) {
    fun withField(field: DnsField, value: String): DnsRecordForm = when (field) {
        DnsField.NAME -> copy(name = value)
        DnsField.CONTENT -> copy(content = value)
        DnsField.TTL -> copy(ttl = value.toIntOrNull() ?: ttl)
        DnsField.PRIORITY -> copy(priority = value)
        DnsField.COMMENT -> copy(comment = value)
        DnsField.CAA_FLAGS -> copy(caaFlags = value)
        DnsField.CAA_TAG -> copy(caaTag = value)
        DnsField.CAA_VALUE -> copy(caaValue = value)
        DnsField.SRV_SERVICE -> copy(srvService = value)
        DnsField.SRV_PROTO -> copy(srvProto = value)
        DnsField.SRV_PRIORITY -> copy(srvPriority = value)
        DnsField.SRV_WEIGHT -> copy(srvWeight = value)
        DnsField.SRV_PORT -> copy(srvPort = value)
        DnsField.SRV_TARGET -> copy(srvTarget = value)
    }

    companion object {
        /** Prefills the editor from an existing record. */
        fun fromRecord(record: DnsRecord): DnsRecordForm {
            val base = DnsRecordForm(
                type = record.type,
                name = record.name,
                content = record.content,
                ttl = record.ttl,
                proxied = record.proxied,
                priority = record.priority?.toString() ?: "10",
                comment = record.displayComment().orEmpty(),
            )
            return when (record.type) {
                DnsRecordType.CAA -> base.copy(
                    content = "",
                    caaFlags = (record.data?.flags ?: 0).toString(),
                    caaTag = record.data?.tag ?: CAA_TAGS.first(),
                    caaValue = record.data?.value.orEmpty(),
                )
                DnsRecordType.SRV -> {
                    val (service, proto, rest) = splitSrvName(record.name)
                    base.copy(
                        name = rest,
                        content = "",
                        srvService = record.data?.service ?: service,
                        srvProto = record.data?.proto ?: proto,
                        srvPriority = (record.data?.priority ?: record.priority ?: 0).toString(),
                        srvWeight = (record.data?.weight ?: 0).toString(),
                        srvPort = record.data?.port?.toString().orEmpty(),
                        srvTarget = record.data?.target.orEmpty(),
                    )
                }
                else -> base
            }
        }

        /**
         * Splits "_sip._tcp.example.com" into ("_sip", "_tcp", "example.com").
         * Names without the two underscore labels come back as ("", "_tcp", name).
         */
        fun splitSrvName(name: String): Triple<String, String, String> {
            val labels = name.split('.')
            return if (labels.size >= 2 && labels[0].startsWith("_") && labels[1].startsWith("_")) {
                Triple(labels[0], labels[1], labels.drop(2).joinToString("."))
            } else {
                Triple("", SRV_PROTOCOLS.first(), name)
            }
        }
    }
}
