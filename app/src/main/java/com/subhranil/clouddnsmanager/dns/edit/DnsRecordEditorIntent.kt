package com.subhranil.clouddnsmanager.dns.edit

import com.subhranil.clouddnsmanager.models.dns.DnsRecordType
import kotlinx.serialization.Serializable

@Serializable
sealed interface DnsRecordEditorIntent {
    /** Only offered when creating; an existing record keeps its type. */
    @Serializable
    data class SetType(val type: DnsRecordType) : DnsRecordEditorIntent

    @Serializable
    data class UpdateField(val field: DnsField, val value: String) : DnsRecordEditorIntent

    @Serializable
    data class SetTtl(val ttl: Int) : DnsRecordEditorIntent

    @Serializable
    data class SetProxied(val proxied: Boolean) : DnsRecordEditorIntent

    @Serializable
    data object Save : DnsRecordEditorIntent

    @Serializable
    data object RetryLoad : DnsRecordEditorIntent

    @Serializable
    data object Back : DnsRecordEditorIntent
}
