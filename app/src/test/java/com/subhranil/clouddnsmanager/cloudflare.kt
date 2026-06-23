package com.subhranil.clouddnsmanager

//
//import com.subhranil.clouddnsmanager.http.CloudflareClient
//import com.subhranil.clouddnsmanager.CloudflareException
//import com.subhranil.clouddnsmanager.models.dns.DnsRecordType
//import com.subhranil.clouddnsmanager.models.token.TokenStatus
//import com.subhranil.clouddnsmanager.models.token.ZoneStatus
//import io.ktor.client.*
//import io.ktor.client.engine.mock.*
//import io.ktor.client.plugins.contentnegotiation.*
//import io.ktor.http.*
//import io.ktor.serialization.kotlinx.json.*
//import kotlinx.coroutines.flow.toList
//import kotlinx.coroutines.test.runTest
//import kotlinx.serialization.json.Json
//import kotlin.test.*
//
//class CloudflareClientTest {
//
//    // ── helpers ──────────────────────────────────────────────────────────────
//
//    private fun mockClient(vararg responses: Pair<String, String>): HttpClient {
//        val queue = ArrayDeque(responses.toList())
//        return HttpClient(MockEngine) {
//            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
//            engine {
//                addHandler { request ->
//                    val (_, body) = queue.removeFirst()
//                    respond(
//                        content = body,
//                        status = HttpStatusCode.OK,
//                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
//                    )
//                }
//            }
//        }
//    }
//
//    private fun cfJson(result: String, extraFields: String = "") =
//        """{"success":true,"errors":[],"messages":[],$extraFields"result":$result}"""
//
//    // ── token tests ──────────────────────────────────────────────────────────
//
//    @Test fun `verifyToken returns active status`() = runTest {
//        val http = mockClient("" to cfJson("""{"id":"tok1","status":"active"}"""))
//        val client = CloudflareClient(token = "test", httpClientOverride = http)
//        val result = client.verifyToken()
//        assertEquals("tok1", result.id)
//        assertEquals(TokenStatus.ACTIVE, result.status)
//    }
//
//    @Test fun `getToken returns policies`() = runTest {
//        val body = cfJson("""
//            {
//              "id":"tok1","name":"My Token","status":"active",
//              "policies":[{
//                "effect":"allow",
//                "resources":{"com.cloudflare.api.account.zone.*":"*"},
//                "permission_groups":[{"id":"pg1","name":"Zone Read"}]
//              }]
//            }
//        """.trimIndent())
//        val http = mockClient("" to body)
//        val client = CloudflareClient(token = "test", httpClientOverride = http)
//        val token = client.getToken("tok1")
//        assertEquals(1, token.policies.size)
//        assertEquals("Zone Read", token.policies[0].permissionGroups[0].name)
//    }
//
//    // ── zone tests ────────────────────────────────────────────────────────────
//
//    @Test fun `listZones returns zones`() = runTest {
//        val body = cfJson(
//            result = """[{"id":"z1","name":"example.com","status":"active","paused":false,
//                          "type":"full","name_servers":[],"account":{"id":"a1","name":"Acme"}}]""",
//            extraFields = """"result_info":{"page":1,"per_page":20,"total_pages":1,"count":1,"total_count":1},""",
//        )
//        val http = mockClient("" to body)
//        val client = CloudflareClient(token = "test", httpClientOverride = http)
//        val page = client.listZones()
//        assertEquals(1, page.items.size)
//        assertEquals("example.com", page.items[0].name)
//        assertEquals(ZoneStatus.ACTIVE, page.items[0].status)
//        assertFalse(page.hasNextPage)
//    }
//
//    @Test fun `allZones collects across multiple pages`() = runTest {
//        fun page(n: Int, total: Int, id: String) = "" to cfJson(
//            result = """[{"id":"$id","name":"$id.com","status":"active","paused":false,
//                          "type":"full","name_servers":[],"account":{"id":"a1","name":"Acme"}}]""",
//            extraFields = """"result_info":{"page":$n,"per_page":1,"total_pages":$total,"count":1,"total_count":$total},""",
//        )
//        val http = mockClient(page(1, 2, "zone1"), page(2, 2, "zone2"))
//        val client = CloudflareClient(token = "test", httpClientOverride = http)
//        val all = client.allZones(perPage = 1).toList()
//        assertEquals(2, all.size)
//        assertEquals("zone1.com", all[0].name)
//        assertEquals("zone2.com", all[1].name)
//    }
//
//    // ── dns record tests ──────────────────────────────────────────────────────
//
//    @Test fun `listDnsRecords returns A record`() = runTest {
//        val body = cfJson(
//            result = """[{"id":"r1","zone_id":"z1","zone_name":"example.com",
//                          "name":"example.com","type":"A","content":"1.2.3.4",
//                          "proxied":true,"ttl":1}]""",
//            extraFields = """"result_info":{"page":1,"per_page":100,"total_pages":1,"count":1,"total_count":1},""",
//        )
//        val http = mockClient("" to body)
//        val client = CloudflareClient(token = "test", httpClientOverride = http)
//        val page = client.listDnsRecords("z1")
//        val record = page.items.single()
//        assertEquals(DnsRecordType.A, record.type)
//        assertEquals("1.2.3.4", record.content)
//        assertTrue(record.proxied)
//    }
//
//    // ── error handling tests ──────────────────────────────────────────────────
//
//    @Test fun `API error response throws CloudflareException ApiError`() = runTest {
//        val http = HttpClient(MockEngine) {
//            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
//            engine {
//                addHandler {
//                    respond(
//                        content = """{"success":false,"errors":[{"code":9109,"message":"Invalid access token"}],"messages":[],"result":null}""",
//                        status = HttpStatusCode.Unauthorized,
//                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
//                    )
//                }
//            }
//        }
//        val client = CloudflareClient(token = "bad-token", httpClientOverride = http)
//        val ex = assertFailsWith<CloudflareException.HttpError> {
//            client.verifyToken()
//        }
//        assertEquals(401, ex.status)
//    }
//}