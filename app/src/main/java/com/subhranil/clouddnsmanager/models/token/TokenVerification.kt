package com.subhranil.clouddnsmanager.models.token

import kotlinx.serialization.Serializable

@Serializable
data class TokenVerification(
    val id: String,
    val status: TokenStatus,
)