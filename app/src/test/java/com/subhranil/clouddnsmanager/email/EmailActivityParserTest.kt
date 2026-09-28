package com.subhranil.clouddnsmanager.email

import com.subhranil.clouddnsmanager.email.api.EmailActivityException
import com.subhranil.clouddnsmanager.email.api.parseEmailActivityResponse
import com.subhranil.clouddnsmanager.email.api.toActivityMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EmailActivityParserTest {

    // Trimmed from Cloudflare's "Querying Email Routing events with GraphQL" tutorial
    private val sample = """
        {
          "data": {
            "viewer": {
              "zones": [
                {
                  "emailRoutingAdaptive": [
                    {
                      "action": "forward",
                      "arc": "none",
                      "datetime": "2026-01-19T10:51:25Z",
                      "dkim": "pass",
                      "dmarc": "pass",
                      "errorDetail": "",
                      "from": "John <john@email.example.com>",
                      "id": "AfWyaZ7V1TAH",
                      "isNDR": 0,
                      "isSpam": 0,
                      "messageId": "<9e6574f1-97f8-4060-ad62-c54b6408ac3f@local>",
                      "spamScore": 0,
                      "spamThreshold": 5,
                      "spf": "pass",
                      "status": "delivered",
                      "subject": "How are you doing?",
                      "to": "me@example.com",
                      "someNewField": {"nested": true}
                    },
                    {
                      "action": "drop",
                      "datetime": "2026-01-19T10:30:00Z",
                      "from": "spam@bad.example",
                      "id": "aYPegrIfLWia",
                      "isSpam": 1,
                      "spamScore": 7.5,
                      "spf": "fail",
                      "status": "dropped",
                      "subject": null,
                      "to": "shop@example.com"
                    }
                  ]
                }
              ]
            }
          },
          "errors": null
        }
    """.trimIndent()

    @Test
    fun `parses events from the sample response`() {
        val events = parseEmailActivityResponse(sample)
        assertEquals(2, events.size)

        val first = events[0]
        assertEquals("AfWyaZ7V1TAH", first.id)
        assertEquals("2026-01-19T10:51:25Z", first.datetime)
        assertEquals("John <john@email.example.com>", first.from)
        assertEquals("me@example.com", first.to)
        assertEquals("How are you doing?", first.subject)
        assertEquals("delivered", first.status)
        assertEquals("forward", first.action)
        assertEquals("pass", first.spf)
        assertEquals("pass", first.dkim)
        assertEquals("pass", first.dmarc)
        assertEquals(0, first.isSpam)

        val second = events[1]
        assertEquals("dropped", second.status)
        assertEquals(null, second.subject)
        assertEquals(null, second.dkim)
        assertEquals(1, second.isSpam)
        assertEquals(7.5, second.spamScore!!, 0.0001)
    }

    @Test
    fun `empty zones or dataset gives an empty list`() {
        assertTrue(parseEmailActivityResponse("""{"data":{"viewer":{"zones":[]}},"errors":null}""").isEmpty())
        assertTrue(parseEmailActivityResponse("""{"data":{"viewer":{"zones":[{"emailRoutingAdaptive":[]}]}}}""").isEmpty())
        assertTrue(parseEmailActivityResponse("""{"data":null,"errors":[]}""").isEmpty())
    }

    @Test
    fun `permission errors are flagged`() {
        val body = """
            {"data":null,"errors":[{"message":"not authorized for that account","path":["viewer","zones","0","emailRoutingAdaptive"],
             "extensions":{"code":"authz","timestamp":"2026-01-19T10:51:25Z"}}]}
        """.trimIndent()
        try {
            parseEmailActivityResponse(body)
            fail("expected an EmailActivityException")
        } catch (e: EmailActivityException) {
            assertTrue(e.isPermissionError)
            assertEquals(listOf("not authorized for that account"), e.messages)
            assertTrue(e.toActivityMessage().contains("Analytics: Read"))
        }
    }

    @Test
    fun `other errors are surfaced even with partial data`() {
        val body = """
            {"data":{"viewer":{"zones":[{"emailRoutingAdaptive":[]}]}},
             "errors":[{"message":"cannot request data older than 7 days","extensions":{"code":"bad_request"}}]}
        """.trimIndent()
        try {
            parseEmailActivityResponse(body)
            fail("expected an EmailActivityException")
        } catch (e: EmailActivityException) {
            assertFalse(e.isPermissionError)
            assertEquals("Cloudflare doesn't keep email activity that far back.", e.toActivityMessage())
        }
    }

    @Test
    fun `unknown errors keep Cloudflare's message`() {
        val e = EmailActivityException(listOf("unknown field \"foo\""), isPermissionError = false)
        assertEquals("Cloudflare couldn't load email activity: unknown field \"foo\"", e.toActivityMessage())
    }
}
