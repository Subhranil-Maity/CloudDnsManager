package com.subhranil.clouddnsmanager.email.model

import kotlinx.serialization.Serializable

/**
 * An Email Routing rule (shown in the app as an "alias").
 *
 * `GET /zones/{zone_id}/email/routing/rules` and `/rules/{rule_identifier}`.
 * A typical alias has one `literal` matcher on the `to` field and one `forward` action.
 */
@Serializable
data class EmailRoutingRule(
    val id: String = "",
    /** Deprecated duplicate of [id]; older responses may only fill this one. */
    val tag: String? = null,
    val name: String? = null,
    val enabled: Boolean = true,
    val priority: Int? = null,
    val matchers: List<EmailRuleMatcher> = emptyList(),
    val actions: List<EmailRuleAction> = emptyList(),
) {
    /** The identifier to use in `/rules/{rule_identifier}` URLs and ItemKeys. */
    val ruleId: String get() = id.ifBlank { tag.orEmpty() }
}

@Serializable
data class EmailRuleMatcher(
    /** "literal" (exact match on [field]) or "all" (catch-all). */
    val type: String = "",
    /** Only "to" is supported by Cloudflare today. */
    val field: String? = null,
    val value: String? = null,
)

@Serializable
data class EmailRuleAction(
    /** "forward", "worker" or "drop". */
    val type: String = "",
    /** Destination addresses for "forward", the Worker name for "worker", empty for "drop". */
    val value: List<String> = emptyList(),
)

/** Body for `POST /rules` and `PUT /rules/{id}`. */
@Serializable
data class EmailRuleRequest(
    val actions: List<EmailRuleAction>,
    val matchers: List<EmailRuleMatcher>,
    val enabled: Boolean = true,
    val name: String? = null,
    val priority: Int? = null,
)

object EmailRuleTypes {
    const val MATCHER_LITERAL = "literal"
    const val MATCHER_ALL = "all"
    const val FIELD_TO = "to"
    const val ACTION_FORWARD = "forward"
    const val ACTION_WORKER = "worker"
    const val ACTION_DROP = "drop"
}
