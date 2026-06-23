package com.subhranil.clouddnsmanager.models.token

import com.subhranil.clouddnsmanager.models.PermissionGroup
import com.subhranil.clouddnsmanager.models.PolicyEffect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenPolicy(
    val effect: PolicyEffect,
    val resources: Map<String, String>,
    @SerialName("permission_groups") val permissionGroups: List<PermissionGroup>,
)