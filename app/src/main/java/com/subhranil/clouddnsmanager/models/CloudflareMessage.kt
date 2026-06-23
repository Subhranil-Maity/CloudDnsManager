package com.subhranil.clouddnsmanager.models

import kotlinx.serialization.Serializable

@Serializable
data class CloudflareMessage(
    val code: Int,
    val message: String,
)