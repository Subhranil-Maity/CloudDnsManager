package com.subhranil.clouddnsmanager.models.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TokenStatus {
    @SerialName("active")   ACTIVE,
    @SerialName("disabled") DISABLED,
    @SerialName("expired")  EXPIRED,
}