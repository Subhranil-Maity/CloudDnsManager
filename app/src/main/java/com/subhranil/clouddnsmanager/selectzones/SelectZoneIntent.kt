package com.subhranil.clouddnsmanager.selectzones

import kotlinx.serialization.Serializable

@Serializable
sealed class SelectZoneIntent{
    @Serializable
    data object DismissError: SelectZoneIntent()

}