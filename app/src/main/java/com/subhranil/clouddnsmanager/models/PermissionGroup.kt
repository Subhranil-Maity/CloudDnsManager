package com.subhranil.clouddnsmanager.models

import kotlinx.serialization.Serializable

@Serializable
data class PermissionGroup(
    val id: String,
    val name: String,
)