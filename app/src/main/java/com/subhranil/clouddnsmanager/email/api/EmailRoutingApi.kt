package com.subhranil.clouddnsmanager.email.api

import com.subhranil.clouddnsmanager.email.model.EmailRoutingSettings
import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.http.CloudflareHttpClient

/**
 * Cloudflare Email Routing API (aliases = routing rules, destination addresses, activity).
 *
 * Built on the shared engine's get/post/put/patch/delete helpers so the email feature never
 * has to edit CloudflareClient. Get one via `sessionManager.clientOrNull()?.emailRoutingApi()`.
 */
internal class EmailRoutingApi(private val engine: CloudflareHttpClient) {

    /** Needs the "Email Routing Rules: Read" (or Edit) token permission. */
    suspend fun getSettings(zoneId: String): EmailRoutingSettings =
        engine.get("/zones/$zoneId/email/routing")
}

internal fun CloudflareClient.emailRoutingApi(): EmailRoutingApi = EmailRoutingApi(engine)
