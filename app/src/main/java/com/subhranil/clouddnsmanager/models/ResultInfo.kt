package com.subhranil.clouddnsmanager.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pagination info from a Cloudflare list response. Some endpoints (e.g. Email Routing)
 * mark every field optional, so each has a default that means "single page".
 */
@Serializable
data class ResultInfo(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1,
    val count: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
)