package com.subhranil.clouddnsmanager.email

/** The zone (and its account) an Email screen or section works on. Passed to ViewModels via Koin. */
data class EmailZone(
    val zoneId: String,
    val zoneName: String,
    /** Destination addresses are account-level in Cloudflare. */
    val accountId: String,
)
