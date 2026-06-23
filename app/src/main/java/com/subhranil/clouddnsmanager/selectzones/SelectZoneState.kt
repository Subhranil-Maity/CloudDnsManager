package com.subhranil.clouddnsmanager.selectzones

import com.subhranil.clouddnsmanager.models.zone.Zone


data class SelectZoneState(
    val zones: List<Zone> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)