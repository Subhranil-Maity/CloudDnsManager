package com.subhranil.clouddnsmanager.models.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ZoneType {
    @SerialName("full")    FULL,
    @SerialName("partial") PARTIAL,
    @SerialName("secondary") SECONDARY,
}