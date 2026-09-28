package com.subhranil.clouddnsmanager.email.model

import kotlinx.serialization.Serializable

/** `GET /zones/{zone_id}/email/routing` — whether Email Routing is on for the zone. */
@Serializable
data class EmailRoutingSettings(
    val id: String? = null,
    val name: String? = null,
    val enabled: Boolean = false,
    /** e.g. "ready", "unconfigured", "misconfigured", "misconfigured/locked", "unlocked". */
    val status: String? = null,
)
