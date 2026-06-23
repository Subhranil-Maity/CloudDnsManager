package com.subhranil.clouddnsmanager.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResultInfo(
    val page: Int,
    @SerialName("per_page") val perPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    val count: Int,
    @SerialName("total_count") val totalCount: Int,
)