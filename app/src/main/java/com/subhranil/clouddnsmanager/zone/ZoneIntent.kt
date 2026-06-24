package com.subhranil.clouddnsmanager.zone

import kotlinx.serialization.Serializable

@Serializable
sealed class ZoneIntent{
    @Serializable
    data object DismissError: ZoneIntent()
}