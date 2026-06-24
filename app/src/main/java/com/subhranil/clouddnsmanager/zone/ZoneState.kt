package com.subhranil.clouddnsmanager.zone

import com.subhranil.clouddnsmanager.models.dns.DnsRecord

data class ZoneState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val dnsRecords: List<DnsRecord> = emptyList()
)