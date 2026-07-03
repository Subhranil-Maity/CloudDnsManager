package com.subhranil.clouddnsmanager.selectzones

import com.subhranil.clouddnsmanager.models.zone.Zone

sealed class SelectZoneDataState {
    data object Loading: SelectZoneDataState()
    data class Error(val message: String): SelectZoneDataState()
    data class ZoneData(val zones: List<Zone>): SelectZoneDataState()
}

data class SelectZoneState(
    val dataState: SelectZoneDataState = SelectZoneDataState.Loading
)