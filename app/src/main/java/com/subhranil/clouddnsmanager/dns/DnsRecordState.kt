package com.subhranil.clouddnsmanager.dns

import com.subhranil.clouddnsmanager.models.dns.DnsRecord

sealed interface DnsRecordDataState {
    data object Loading : DnsRecordDataState
    data class Error(val error: String) : DnsRecordDataState
    data class DnsRecordData(val dnsList: List<DnsRecord>) : DnsRecordDataState
}

data class DnsRecordState(
    val dnsRecordDataState: DnsRecordDataState = DnsRecordDataState.Loading,
    val openDetailedDrawer: DnsRecord? = null,
)