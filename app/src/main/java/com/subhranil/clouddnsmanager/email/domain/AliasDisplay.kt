package com.subhranil.clouddnsmanager.email.domain

import com.subhranil.clouddnsmanager.email.model.EmailRoutingRule
import com.subhranil.clouddnsmanager.email.model.EmailRuleAction
import com.subhranil.clouddnsmanager.email.model.EmailRuleMatcher
import com.subhranil.clouddnsmanager.email.model.EmailRuleRequest
import com.subhranil.clouddnsmanager.email.model.EmailRuleTypes

/** What happens to mail sent to an alias. */
sealed interface AliasTarget {
    data class Forward(val destinations: List<String>) : AliasTarget
    data class Worker(val worker: String) : AliasTarget
    data object Drop : AliasTarget
    /** No action, or one this app doesn't know. */
    data class Unknown(val type: String?) : AliasTarget
}

/** A routing rule reduced to what the alias list and details need. */
data class AliasDisplay(
    val ruleId: String,
    /** The literal `to` address, e.g. "quiet-river-4821@example.com". Null for catch-all / odd rules. */
    val address: String?,
    val name: String?,
    val enabled: Boolean,
    val isCatchAll: Boolean,
    val target: AliasTarget,
) {
    /** The address when there is one, otherwise the rule name (or a generic label). */
    val title: String get() = address ?: name?.takeIf { it.isNotBlank() } ?: if (isCatchAll) "Catch-all" else "Rule"

    /** One-line summary of [target], e.g. "→ me@example.com" or "Drop". */
    val targetSummary: String get() = when (target) {
        is AliasTarget.Forward -> if (target.destinations.isEmpty()) "Forward (no destination)"
        else "→ " + target.destinations.joinToString(", ")
        is AliasTarget.Worker -> "Worker: ${target.worker}"
        AliasTarget.Drop -> "Drop"
        is AliasTarget.Unknown -> target.type?.let { "Action: $it" } ?: "No action"
    }

    /** True when this alias is a simple literal-to-forward rule the app can edit. */
    val isEditableForward: Boolean get() = address != null && !isCatchAll && target is AliasTarget.Forward

    fun forwardsTo(email: String): Boolean =
        target is AliasTarget.Forward && target.destinations.any { it.equals(email, ignoreCase = true) }

    fun matchesQuery(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return listOfNotNull(address, name, targetSummary).any { it.contains(q, ignoreCase = true) }
    }
}

/** Pure mapping from Cloudflare's matchers/actions to the alias shown in the UI. */
fun EmailRoutingRule.toAliasDisplay(): AliasDisplay {
    val isCatchAll = matchers.any { it.type == EmailRuleTypes.MATCHER_ALL }
    val address = matchers.firstOrNull {
        it.type == EmailRuleTypes.MATCHER_LITERAL && (it.field == null || it.field == EmailRuleTypes.FIELD_TO)
    }?.value?.takeIf { it.isNotBlank() }

    val action = actions.firstOrNull()
    val target = when (action?.type) {
        EmailRuleTypes.ACTION_FORWARD -> AliasTarget.Forward(actions
            .filter { it.type == EmailRuleTypes.ACTION_FORWARD }
            .flatMap { it.value }
            .filter { it.isNotBlank() })
        EmailRuleTypes.ACTION_WORKER -> AliasTarget.Worker(action.value.firstOrNull().orEmpty())
        EmailRuleTypes.ACTION_DROP -> AliasTarget.Drop
        else -> AliasTarget.Unknown(action?.type)
    }
    return AliasDisplay(
        ruleId = ruleId,
        address = address,
        name = name,
        enabled = enabled,
        isCatchAll = isCatchAll,
        target = target,
    )
}

/** A new alias: `to` [address] is forwarded to [destination]. */
fun newForwardRule(address: String, destination: String, name: String?): EmailRuleRequest =
    EmailRuleRequest(
        matchers = listOf(EmailRuleMatcher(EmailRuleTypes.MATCHER_LITERAL, EmailRuleTypes.FIELD_TO, address)),
        actions = listOf(EmailRuleAction(EmailRuleTypes.ACTION_FORWARD, listOf(destination))),
        enabled = true,
        name = name?.trim()?.takeIf { it.isNotEmpty() },
    )

/**
 * The PUT body for an existing rule with some fields changed. PUT replaces the whole rule,
 * so everything not being changed is copied from [this].
 */
fun EmailRoutingRule.toUpdateRequest(
    enabled: Boolean = this.enabled,
    name: String? = this.name,
    forwardTo: String? = null,
): EmailRuleRequest = EmailRuleRequest(
    matchers = matchers,
    actions = if (forwardTo != null) listOf(EmailRuleAction(EmailRuleTypes.ACTION_FORWARD, listOf(forwardTo))) else actions,
    enabled = enabled,
    name = name?.trim()?.takeIf { it.isNotEmpty() },
    priority = priority,
)
