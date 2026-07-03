package com.subhranil.clouddnsmanager.dns

import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import kotlinx.serialization.Serializable

@Serializable
sealed interface DnsRecordIntent {
    @Serializable
    data object DismissDetailedDrawer : DnsRecordIntent

    @Serializable
    data class ShowDetailed(val record: DnsRecord) : DnsRecordIntent

    @Serializable
    data object Retry : DnsRecordIntent

    @Serializable
    data object GoBack : DnsRecordIntent
}