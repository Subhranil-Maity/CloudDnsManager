package com.subhranil.clouddnsmanager.models.dns

import kotlinx.serialization.Serializable


/**
 * Catch-all bag for structured record data (SRV, LOC, CAA, etc.).
 *
 * Also used as the `data` object of write requests: null fields are left out of the JSON
 * (the shared Json config doesn't encode defaults), so a CAA request only sends
 * flags/tag/value and an SRV request only sends priority/weight/port/target.
 */
@Serializable
data class DnsRecordData(
    val service: String? = null,
    val proto: String? = null,
    val name: String? = null,
    val priority: Int? = null,
    val weight: Int? = null,
    val port: Int? = null,
    val target: String? = null,
    /** CAA flags (0-255). */
    val flags: Int? = null,
    val tag: String? = null,
    val value: String? = null,
)
