package com.subhranil.clouddnsmanager.email.home

sealed interface EmailHomeIntent {
    data object Retry : EmailHomeIntent
    data object Back : EmailHomeIntent
}
