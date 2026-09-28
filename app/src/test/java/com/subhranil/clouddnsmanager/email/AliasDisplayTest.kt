package com.subhranil.clouddnsmanager.email

import com.subhranil.clouddnsmanager.email.domain.AliasTarget
import com.subhranil.clouddnsmanager.email.domain.newForwardRule
import com.subhranil.clouddnsmanager.email.domain.toAliasDisplay
import com.subhranil.clouddnsmanager.email.domain.toUpdateRequest
import com.subhranil.clouddnsmanager.email.model.EmailRoutingRule
import com.subhranil.clouddnsmanager.email.model.EmailRuleAction
import com.subhranil.clouddnsmanager.email.model.EmailRuleMatcher
import com.subhranil.clouddnsmanager.http.cfJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AliasDisplayTest {

    private fun rule(
        matchers: List<EmailRuleMatcher>,
        actions: List<EmailRuleAction>,
        enabled: Boolean = true,
        name: String? = null,
    ) = EmailRoutingRule(id = "r1", name = name, enabled = enabled, priority = 3, matchers = matchers, actions = actions)

    private val toMatcher = EmailRuleMatcher("literal", "to", "shop@example.com")

    @Test
    fun `forward rule shows address and destinations`() {
        val display = rule(listOf(toMatcher), listOf(EmailRuleAction("forward", listOf("me@gmail.com")))).toAliasDisplay()
        assertEquals("shop@example.com", display.address)
        assertEquals(AliasTarget.Forward(listOf("me@gmail.com")), display.target)
        assertEquals("→ me@gmail.com", display.targetSummary)
        assertTrue(display.isEditableForward)
        assertTrue(display.forwardsTo("ME@gmail.com"))
        assertFalse(display.isCatchAll)
    }

    @Test
    fun `multiple forward values are all listed`() {
        val display = rule(
            listOf(toMatcher),
            listOf(EmailRuleAction("forward", listOf("a@x.com", "b@x.com"))),
        ).toAliasDisplay()
        assertEquals(AliasTarget.Forward(listOf("a@x.com", "b@x.com")), display.target)
        assertEquals("→ a@x.com, b@x.com", display.targetSummary)
    }

    @Test
    fun `drop and worker actions`() {
        val drop = rule(listOf(toMatcher), listOf(EmailRuleAction("drop"))).toAliasDisplay()
        assertEquals(AliasTarget.Drop, drop.target)
        assertEquals("Drop", drop.targetSummary)
        assertFalse(drop.isEditableForward)

        val worker = rule(listOf(toMatcher), listOf(EmailRuleAction("worker", listOf("my-worker")))).toAliasDisplay()
        assertEquals(AliasTarget.Worker("my-worker"), worker.target)
        assertEquals("Worker: my-worker", worker.targetSummary)
    }

    @Test
    fun `catch-all has no address`() {
        val display = rule(listOf(EmailRuleMatcher("all")), listOf(EmailRuleAction("drop"))).toAliasDisplay()
        assertTrue(display.isCatchAll)
        assertNull(display.address)
        assertEquals("Catch-all", display.title)
    }

    @Test
    fun `missing actions and unknown types`() {
        val none = rule(listOf(toMatcher), emptyList(), name = "Old").toAliasDisplay()
        assertEquals(AliasTarget.Unknown(null), none.target)
        assertEquals("No action", none.targetSummary)

        val odd = rule(listOf(EmailRuleMatcher("regex", "subject", "x")), listOf(EmailRuleAction("teleport")), name = "Odd").toAliasDisplay()
        assertNull(odd.address)
        assertEquals("Odd", odd.title)
        assertEquals(AliasTarget.Unknown("teleport"), odd.target)
    }

    @Test
    fun `search matches address, name and destination`() {
        val display = rule(listOf(toMatcher), listOf(EmailRuleAction("forward", listOf("me@gmail.com"))), name = "Shopping").toAliasDisplay()
        assertTrue(display.matchesQuery(""))
        assertTrue(display.matchesQuery("SHOP@"))
        assertTrue(display.matchesQuery("shopping"))
        assertTrue(display.matchesQuery("gmail"))
        assertFalse(display.matchesQuery("bank"))
    }

    @Test
    fun `parses a Cloudflare rules list item`() {
        val json = """
            {"id":"a7e6fb77503c41d8a7f3113c6918f10c","tag":"a7e6fb77503c41d8a7f3113c6918f10c",
             "name":"Send to user@example.net rule.","enabled":true,"priority":0,"source":"api",
             "matchers":[{"type":"literal","field":"to","value":"test@example.com"}],
             "actions":[{"type":"forward","value":["destinationaddress@example.net"]}]}
        """.trimIndent()
        val display = cfJson.decodeFromString(EmailRoutingRule.serializer(), json).toAliasDisplay()
        assertEquals("a7e6fb77503c41d8a7f3113c6918f10c", display.ruleId)
        assertEquals("test@example.com", display.address)
        assertEquals(AliasTarget.Forward(listOf("destinationaddress@example.net")), display.target)
    }

    @Test
    fun `rule id falls back to the deprecated tag`() {
        val rule = EmailRoutingRule(id = "", tag = "legacy")
        assertEquals("legacy", rule.ruleId)
    }

    @Test
    fun `new forward rule and update requests`() {
        val created = newForwardRule("new@example.com", "me@gmail.com", "  ")
        assertEquals(listOf(EmailRuleMatcher("literal", "to", "new@example.com")), created.matchers)
        assertEquals(listOf(EmailRuleAction("forward", listOf("me@gmail.com"))), created.actions)
        assertNull(created.name)

        val existing = rule(listOf(toMatcher), listOf(EmailRuleAction("forward", listOf("old@x.com"))), name = "Shop")
        val disabled = existing.toUpdateRequest(enabled = false)
        assertFalse(disabled.enabled)
        assertEquals(existing.actions, disabled.actions)
        assertEquals("Shop", disabled.name)
        assertEquals(3, disabled.priority)

        val moved = existing.toUpdateRequest(forwardTo = "new@x.com", name = "Renamed")
        assertEquals(listOf(EmailRuleAction("forward", listOf("new@x.com"))), moved.actions)
        assertEquals("Renamed", moved.name)
        assertEquals(existing.matchers, moved.matchers)
    }
}
