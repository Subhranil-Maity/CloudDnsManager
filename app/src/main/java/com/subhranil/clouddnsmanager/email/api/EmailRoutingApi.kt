package com.subhranil.clouddnsmanager.email.api

import com.subhranil.clouddnsmanager.CloudflareException
import com.subhranil.clouddnsmanager.email.model.CreateAddressRequest
import com.subhranil.clouddnsmanager.email.model.EmailActivityEvent
import com.subhranil.clouddnsmanager.email.model.EmailDeleteResult
import com.subhranil.clouddnsmanager.email.model.EmailDestinationAddress
import com.subhranil.clouddnsmanager.email.model.EmailRoutingRule
import com.subhranil.clouddnsmanager.email.model.EmailRoutingSettings
import com.subhranil.clouddnsmanager.email.model.EmailRuleRequest
import com.subhranil.clouddnsmanager.http.CloudflareClient
import com.subhranil.clouddnsmanager.http.CloudflareHttpClient
import com.subhranil.clouddnsmanager.http.apiUrl
import com.subhranil.clouddnsmanager.http.cfJson
import com.subhranil.clouddnsmanager.models.CloudflareResponse
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Cloudflare Email Routing API (aliases = routing rules, destination addresses, activity).
 *
 * Built on the shared engine's get/post/put/patch/delete helpers so the email feature never
 * has to edit CloudflareClient. Get one via `sessionManager.clientOrNull()?.emailRoutingApi()`.
 *
 * Token permissions: "Email Routing Rules" (zone) for settings and rules,
 * "Email Routing Addresses" (account) for destination addresses, "Analytics: Read" for activity.
 */
internal class EmailRoutingApi(private val engine: CloudflareHttpClient) {

    /** Needs the "Email Routing Rules: Read" (or Edit) token permission. */
    suspend fun getSettings(zoneId: String): EmailRoutingSettings =
        engine.get("/zones/$zoneId/email/routing")

    // ── Rules (aliases) ─────────────────────────────────────────────────────

    /** Every routing rule in the zone, following pagination (Cloudflare allows at most 50 per page). */
    suspend fun listRules(zoneId: String): List<EmailRoutingRule> =
        allPages { page -> engine.getEnvelope("/zones/$zoneId/email/routing/rules", page) }

    suspend fun getRule(zoneId: String, ruleId: String): EmailRoutingRule =
        engine.get("/zones/$zoneId/email/routing/rules/$ruleId")

    /** The catch-all rule (matcher type "all"). Shown read-only in the app. */
    suspend fun getCatchAll(zoneId: String): EmailRoutingRule =
        engine.get("/zones/$zoneId/email/routing/rules/catch_all")

    suspend fun createRule(zoneId: String, rule: EmailRuleRequest): EmailRoutingRule =
        engine.post("/zones/$zoneId/email/routing/rules", rule)

    /** PUT replaces the whole rule, so always send every field (matchers, actions, enabled, name). */
    suspend fun updateRule(zoneId: String, ruleId: String, rule: EmailRuleRequest): EmailRoutingRule =
        engine.put("/zones/$zoneId/email/routing/rules/$ruleId", rule)

    suspend fun deleteRule(zoneId: String, ruleId: String) {
        engine.delete<EmailDeleteResult>("/zones/$zoneId/email/routing/rules/$ruleId")
    }

    // ── Destination addresses (account-level) ────────────────────────────────

    suspend fun listAddresses(accountId: String): List<EmailDestinationAddress> =
        allPages { page -> engine.getEnvelope("/accounts/$accountId/email/routing/addresses", page) }

    /** Cloudflare emails a verification link; the address can't be used until it's clicked. */
    suspend fun createAddress(accountId: String, email: String): EmailDestinationAddress =
        engine.post("/accounts/$accountId/email/routing/addresses", CreateAddressRequest(email))

    suspend fun deleteAddress(accountId: String, addressId: String) {
        engine.delete<EmailDeleteResult>("/accounts/$accountId/email/routing/addresses/$addressId")
    }

    // ── Activity (GraphQL Analytics) ─────────────────────────────────────────

    /**
     * Recent incoming mail for the zone, newest first. [to] narrows it to one alias.
     * Uses the GraphQL endpoint, whose response is `{ data, errors }` rather than the v4 envelope.
     */
    suspend fun getActivity(
        zoneId: String,
        since: Instant,
        until: Instant = Instant.now(),
        to: String? = null,
        limit: Int = ACTIVITY_LIMIT,
    ): List<EmailActivityEvent> {
        val request = GraphqlRequest(
            query = emailActivityQuery(limit),
            variables = buildJsonObject {
                put("zoneTag", zoneId)
                put("filter", buildJsonObject {
                    put("datetime_geq", JsonPrimitive(since.truncatedTo(ChronoUnit.SECONDS).toString()))
                    put("datetime_leq", JsonPrimitive(until.truncatedTo(ChronoUnit.SECONDS).toString()))
                    if (to != null) put("to", JsonPrimitive(to))
                })
            },
        )
        val body: String
        val status: Int
        try {
            val response = engine.http.post(apiUrl("/graphql")) { setBody(request) }
            status = response.status.value
            body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                // A GraphQL error body explains more than the bare status; otherwise fall back to it.
                val graphqlError = runCatching { parseEmailActivityResponse(body, cfJson) }.exceptionOrNull()
                if (graphqlError is EmailActivityException) throw graphqlError
                throw CloudflareException.HttpError(status, body)
            }
        } catch (e: IOException) {
            throw CloudflareException.NetworkError(e)
        }
        return try {
            parseEmailActivityResponse(body, cfJson)
        } catch (e: EmailActivityException) {
            throw e
        } catch (e: Exception) {
            throw CloudflareException.DeserializationError(e)
        }
    }

    /**
     * Fetches page after page until Cloudflare says there are no more. Also stops on a short
     * page, in case `result_info` is missing.
     */
    private suspend fun <T> allPages(
        fetch: suspend (List<Pair<String, String>>) -> CloudflareResponse<List<T>>,
    ): List<T> {
        val all = mutableListOf<T>()
        var page = 1
        while (page <= MAX_PAGES) {
            val envelope = fetch(listOf("page" to page.toString(), "per_page" to PER_PAGE.toString()))
            val items = envelope.result.orEmpty()
            all += items
            val info = envelope.resultInfo
            val hasMore = if (info != null) info.page < info.totalPages else items.size >= PER_PAGE
            if (!hasMore || items.isEmpty()) break
            page++
        }
        return all
    }

    companion object {
        const val PER_PAGE = 50
        private const val MAX_PAGES = 100
        const val ACTIVITY_LIMIT = 200
    }
}

internal fun CloudflareClient.emailRoutingApi(): EmailRoutingApi = EmailRoutingApi(engine)
