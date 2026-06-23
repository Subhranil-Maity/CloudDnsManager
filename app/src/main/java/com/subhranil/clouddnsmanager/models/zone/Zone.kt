package com.subhranil.clouddnsmanager.models.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Zone(
    val id: String,
    val name: String,
    val status: ZoneStatus,
    val paused: Boolean,
    val type: ZoneType,
    @SerialName("name_servers") val nameServers: List<String> = emptyList(),
    @SerialName("original_name_servers") val originalNameServers: List<String> = emptyList(),
    val account: ZoneAccount,
    val plan: ZonePlan? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("modified_on") val modifiedOn: String? = null,
)