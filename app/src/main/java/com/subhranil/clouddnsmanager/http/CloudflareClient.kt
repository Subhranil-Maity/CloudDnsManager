package com.subhranil.clouddnsmanager.http

import com.subhranil.clouddnsmanager.*
import com.subhranil.clouddnsmanager.models.token.*
import com.subhranil.clouddnsmanager.models.*
import com.subhranil.clouddnsmanager.models.zone.*
import com.subhranil.clouddnsmanager.models.dns.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Idiomatic Kotlin client for the Cloudflare v4 API.
 *
 * Usage:
 * ```kotlin
 * val cf = CloudflareClient(token = "your-api-token")
 * val verification = cf.verifyToken()
 * cf.allZones().collect { zone -> println(zone.name) }
 * val records = cf.listDnsRecords(zoneId = "abc123")
 * cf.close()
 * ```
 *
 * The client is [AutoCloseable]; wrap in `use {}` or call [close] when done.
 *
 * ### URL construction note
 * Ktor's `defaultRequest { url(...) }` uses RFC-3986 reference resolution, which
 * means a call-site path beginning with "/" is treated as an *absolute* path and
 * silently drops the "/client/v4" prefix from the base URL.
 * We work around this by calling [apiUrl] at every call-site to build full URLs
 * before passing them to Ktor.
 */
class CloudflareClient(
    token: String,
    logLevel: LogLevel = LogLevel.NONE,
    /** Supply a custom [HttpClient] for testing (e.g. MockEngine). */
    httpClientOverride: HttpClient? = null,
) : AutoCloseable {

    private val engine = CloudflareHttpClient(token, logLevel, httpClientOverride)

    // ──────────────────────────────────────────
    // Token & Permissions
    // ──────────────────────────────────────────

    /** Verify the token is valid and return its status. Lightweight health-check. */
    suspend fun verifyToken(): TokenVerification =
        engine.get("/user/tokens/verify")

    /** Return full token details including all permission policies. */
    suspend fun getToken(tokenId: String): Token =
        engine.get("/user/tokens/$tokenId")

    // ──────────────────────────────────────────
    // Zones
    // ──────────────────────────────────────────

    /**
     * Return a single page of zones.
     *
     * @param filter     Optional filters (name, status, account).
     * @param pagination Page + page-size controls.
     */
    suspend fun listZones(
        filter: ZoneFilter = ZoneFilter(),
        pagination: PaginationParams = PaginationParams(),
    ): Page<Zone> {
        val params = filter.toQueryParams() + pagination.toQueryParams()
        // Use engine.http directly so we can decode the full envelope (including
        // result_info) in one request — engine.get() discards the envelope wrapper.
        val response = engine.http.get(apiUrl("/zones")) {
            params.forEach { (k, v) -> parameter(k, v) }
        }
        val envelope = cfJson.decodeFromString(
            CloudflareResponse.serializer(
                kotlinx.serialization.builtins.ListSerializer(Zone.serializer())
            ),
            response.body<String>()
        )
        return Page(envelope.result.orEmpty(), envelope.resultInfo)
    }

    /** Fetch a single zone by ID. */
    suspend fun getZone(zoneId: String): Zone =
        engine.get("/zones/$zoneId")

    /**
     * Lazily stream *all* zones across every page as a [Flow].
     * Pagination is handled automatically.
     *
     * ```kotlin
     * cf.allZones().collect { zone -> println(zone.name) }
     * ```
     */
    fun allZones(
        filter: ZoneFilter = ZoneFilter(),
        perPage: Int = 50,
    ): Flow<Zone> = paginatedFlow(perPage) { pagination ->
        listZones(filter, pagination)
    }

    // ──────────────────────────────────────────
    // DNS Records
    // ──────────────────────────────────────────

    /**
     * Return a single page of DNS records for [zoneId].
     *
     * @param zoneId     Zone ID from [listZones] / [allZones].
     * @param filter     Optional filters (type, name, content, proxied).
     * @param pagination Page + page-size controls.
     */
    suspend fun listDnsRecords(
        zoneId: String,
        filter: DnsRecordFilter = DnsRecordFilter(),
        pagination: PaginationParams = PaginationParams(),
    ): Page<DnsRecord> {
        val params = filter.toQueryParams() + pagination.toQueryParams()
        val response = engine.http.get(apiUrl("/zones/$zoneId/dns_records")) {
            params.forEach { (k, v) -> parameter(k, v) }
        }
        val envelope = cfJson.decodeFromString(
            CloudflareResponse.serializer(
                kotlinx.serialization.builtins.ListSerializer(DnsRecord.serializer())
            ),
            response.body<String>()
        )
        return Page(envelope.result.orEmpty(), envelope.resultInfo)
    }

    /**
     * Lazily stream *all* DNS records for [zoneId] as a [Flow].
     *
     * ```kotlin
     * cf.allDnsRecords(zoneId).collect { record -> println("${record.type} ${record.name}") }
     * ```
     */
    fun allDnsRecords(
        zoneId: String,
        filter: DnsRecordFilter = DnsRecordFilter(),
        perPage: Int = 100,
    ): Flow<DnsRecord> = paginatedFlow(perPage) { pagination ->
        listDnsRecords(zoneId, filter, pagination)
    }

    /** Fetch a single DNS record by [recordId]. */
    suspend fun getDnsRecord(zoneId: String, recordId: String): DnsRecord =
        engine.get("/zones/$zoneId/dns_records/$recordId")

    // ──────────────────────────────────────────
    // Internals
    // ──────────────────────────────────────────

    /** Generic auto-paginating flow builder. */
    private fun <T> paginatedFlow(
        perPage: Int,
        fetch: suspend (PaginationParams) -> Page<T>,
    ): Flow<T> = flow {
        var page = 1
        while (true) {
            val result = fetch(PaginationParams(page = page, perPage = perPage))
            result.items.forEach { emit(it) }
            if (!result.hasNextPage) break
            page++
        }
    }

    override fun close() = engine.close()
}
