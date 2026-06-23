package com.subhranil.clouddnsmanager.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudflareResponse<T>(
    val result: T? = null,
    val success: Boolean,
    val errors: List<CloudflareError> = emptyList(),
    val messages: List<CloudflareMessage> = emptyList(),
    @SerialName("result_info") val resultInfo: ResultInfo? = null,
)