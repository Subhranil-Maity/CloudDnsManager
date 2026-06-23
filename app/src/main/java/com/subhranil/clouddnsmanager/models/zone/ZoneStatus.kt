package com.subhranil.clouddnsmanager.models.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ZoneStatus {
    @SerialName("active")      ACTIVE,
    @SerialName("pending")     PENDING,
    @SerialName("initializing") INITIALIZING,
    @SerialName("moved")       MOVED,
    @SerialName("deleted")     DELETED,
    @SerialName("deactivated") DEACTIVATED,
    @SerialName("read only")   READ_ONLY,
}