package com.subhranil.clouddnsmanager.selectzones

import kotlinx.serialization.Serializable

@Serializable
sealed class SelectZoneIntent{
//    @Serializable
//    data object DismissError: SelectZoneIntent()
    @Serializable
    data class SelectZone(val zoneId: String, val zoneName: String, val accountId: String): SelectZoneIntent()
    @Serializable
    data object Retry: SelectZoneIntent()
    @Serializable
    data object RequestLogout: SelectZoneIntent()
    @Serializable
    data object DismissLogout: SelectZoneIntent()
    @Serializable
    data object ConfirmLogout: SelectZoneIntent()
    @Serializable
    data object OpenSecurity: SelectZoneIntent()

}