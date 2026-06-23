package com.subhranil.clouddnsmanager.models.dns

import kotlinx.serialization.Serializable


/** Catch-all bag for structured record data (SRV, LOC, CAA, etc.) */
@Serializable
data class DnsRecordData(
    val service: String? = null,
    val proto: String? = null,
    val name: String? = null,
    val priority: Int? = null,
    val weight: Int? = null,
    val port: Int? = null,
    val target: String? = null,
    val tag: String? = null,
    val value: String? = null,
)