package com.subhranil.clouddnsmanager.models.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Token(
    val id: String,
    val name: String,
    val status: TokenStatus,
    val policies: List<TokenPolicy> = emptyList(),
    @SerialName("not_before") val notBefore: String? = null,
    @SerialName("expires_on") val expiresOn: String? = null,
)