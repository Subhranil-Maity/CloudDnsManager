package com.subhranil.clouddnsmanager.onboading

data class OnBoardingState(
    val token: String = "",
    val isTokenVerifying: Boolean = false,
    val isTokenVerified: Boolean = false,
    val error: String? = null
)