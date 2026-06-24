package com.subhranil.clouddnsmanager.zone

import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import kotlinx.serialization.Serializable

@Serializable
sealed class ZoneIntent{
    @Serializable
    data object DismissError: ZoneIntent()
    @Serializable
    data object DismissDetailedDrawer: ZoneIntent()
    @Serializable
    data class ShowDetailed(val record: DnsRecord): ZoneIntent()
}