package com.subhranil.clouddnsmanager.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PolicyEffect {
    @SerialName("allow") ALLOW,
    @SerialName("deny")  DENY,
}