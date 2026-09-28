package com.subhranil.clouddnsmanager.email.model

import kotlinx.serialization.Serializable

/**
 * One incoming email from the GraphQL `emailRoutingAdaptive` dataset.
 * Field names follow Cloudflare's "Querying Email Routing events" tutorial
 * (`id` is aliased from `sessionId` in the query).
 */
@Serializable
data class EmailActivityEvent(
    val id: String? = null,
    val datetime: String = "",
    val messageId: String? = null,
    val from: String? = null,
    val to: String? = null,
    val subject: String? = null,
    /** e.g. "delivered", "dropped", "deliveryFailed", "rejected". */
    val status: String? = null,
    /** e.g. "forward", "drop", "worker". */
    val action: String? = null,
    val spf: String? = null,
    val dkim: String? = null,
    val dmarc: String? = null,
    val arc: String? = null,
    val errorDetail: String? = null,
    val isNDR: Int = 0,
    val isSpam: Int = 0,
    val spamScore: Double? = null,
    val spamThreshold: Double? = null,
)
