package com.subhranil.clouddnsmanager.models

import kotlinx.serialization.Serializable

@Serializable
data class CloudflareError(
    val code: Int,
    val message: String,
)