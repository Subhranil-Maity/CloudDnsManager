package com.subhranil.clouddnsmanager.email.api

import com.subhranil.clouddnsmanager.email.model.EmailActivityEvent
import com.subhranil.clouddnsmanager.toUserMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * GraphQL Analytics query for the zone's Email Routing activity log.
 * Fields are exactly those of Cloudflare's "Querying Email Routing events" tutorial:
 * asking for a field the dataset doesn't have fails the whole query.
 */
internal fun emailActivityQuery(limit: Int): String = """
query EmailRoutingActivity(${'$'}zoneTag: string, ${'$'}filter: EmailRoutingAdaptiveFilter_InputObject) {
  viewer {
    zones(filter: { zoneTag: ${'$'}zoneTag }) {
      emailRoutingAdaptive(filter: ${'$'}filter, limit: ${limit.coerceIn(1, 10_000)}, orderBy: [datetime_DESC]) {
        datetime
        id: sessionId
        messageId
        from
        to
        subject
        status
        action
        spf
        dkim
        dmarc
        arc
        errorDetail
        isNDR
        isSpam
        spamThreshold
        spamScore
      }
    }
  }
}
"""

/** `{ query, variables }` body POSTed to `/graphql`. */
@Serializable
internal data class GraphqlRequest(
    val query: String,
    val variables: JsonObject,
)

/**
 * The GraphQL API answered, but with errors (it returns HTTP 200 with `errors` set, not the v4 envelope).
 * [isPermissionError] is true when the token lacks the Analytics permission.
 */
class EmailActivityException(
    val messages: List<String>,
    val isPermissionError: Boolean,
) : Exception(messages.joinToString("; ").ifBlank { "GraphQL error" })

private val PERMISSION_HINTS = listOf("not authorized", "unauthorized", "authz", "permission", "access denied", "does not have access")

/**
 * Pure parser for the GraphQL response `{ data: { viewer: { zones: [ { emailRoutingAdaptive: [...] } ] } }, errors }`.
 *
 * @throws EmailActivityException when `errors` is non-empty (even if partial data came back).
 */
fun parseEmailActivityResponse(body: String, json: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }): List<EmailActivityEvent> {
    val root = json.parseToJsonElement(body).jsonObject

    val errors = (root["errors"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    if (errors.isNotEmpty()) {
        val messages = errors.map { (it["message"] as? JsonPrimitive)?.contentOrNull.orEmpty() }
        val codes = errors.mapNotNull { error ->
            ((error["extensions"] as? JsonObject)?.get("code") as? JsonPrimitive)?.contentOrNull
        }
        val permission = (messages + codes).any { text ->
            PERMISSION_HINTS.any { hint -> text.contains(hint, ignoreCase = true) }
        }
        throw EmailActivityException(messages.filter { it.isNotBlank() }, permission)
    }

    val zones = root.path("data", "viewer", "zones") as? JsonArray ?: return emptyList()
    return zones.flatMap { zone ->
        val events = (zone as? JsonObject)?.get("emailRoutingAdaptive") as? JsonArray ?: return@flatMap emptyList()
        events.map { json.decodeFromJsonElement(EmailActivityEvent.serializer(), it) }
    }
}

private fun JsonObject.path(vararg keys: String): JsonElement? {
    var current: JsonElement? = this
    for (key in keys) {
        current = (current as? JsonObject)?.get(key)
        if (current == null || current is JsonNull) return null
    }
    return current
}

/** Turns an activity-log failure into UI text; falls back to the shared v4 mapping. */
fun Throwable.toActivityMessage(): String = when (this) {
    is EmailActivityException -> when {
        isPermissionError ->
            "Your API token can't read email activity. Add the \"Analytics: Read\" permission to the token in the Cloudflare dashboard."
        messages.any { it.contains("time range", ignoreCase = true) || it.contains("older than", ignoreCase = true) } ->
            "Cloudflare doesn't keep email activity that far back."
        else -> messages.firstOrNull()?.let { "Cloudflare couldn't load email activity: $it" }
            ?: "Cloudflare couldn't load email activity."
    }
    else -> toUserMessage(permissionHint = "Analytics: Read")
}
