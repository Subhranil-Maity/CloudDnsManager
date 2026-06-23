package com.subhranil.clouddnsmanager.models.zone

import kotlinx.serialization.Serializable

@Serializable
data class ZoneAccount(
    val id: String,
    val name: String,
)