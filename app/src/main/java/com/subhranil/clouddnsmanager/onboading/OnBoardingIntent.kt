package com.subhranil.clouddnsmanager.onboading

sealed class OnBoardingIntent {
    data class UpdateToken(val token: String): OnBoardingIntent()
    data object VerifyToken: OnBoardingIntent()
    data object DismissError: OnBoardingIntent()
    data object Continue: OnBoardingIntent()
}