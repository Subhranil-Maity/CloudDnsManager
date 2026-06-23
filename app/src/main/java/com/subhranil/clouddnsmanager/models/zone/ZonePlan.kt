package com.subhranil.clouddnsmanager.models.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZonePlan(
    val id: String? = null,
    val name: String,
    @SerialName("price") val price: Int = 0,
    val currency: String = "USD",
)