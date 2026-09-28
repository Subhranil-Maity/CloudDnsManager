package com.subhranil.clouddnsmanager.http


import android.util.Log
import com.subhranil.clouddnsmanager.CloudflareException
import com.subhranil.clouddnsmanager.models.CloudflareResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.IOException

internal const val BASE_URL = "https://api.cloudflare.com/client/v4"

// Ktor's defaultRequest { url(...) } treats paths starting with "/" as absolute
// (stripping the "/client/v4" prefix). We fix this by building full URLs at
// call-site instead of relying on base-URL inheritance.
internal fun apiUrl(path: String): String {
    // path may or may not start with "/"
    val cleanPath = if (path.startsWith("/")) path else "/$path"
    return "$BASE_URL$cleanPath"
}

/** Lenient JSON config: ignores unknown keys so new CF fields never break parsing. */
internal val cfJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues  = true   // maps unknown enum values to defaults
    isLenient          = true
}

/**
 * Thin wrapper around an [HttpClient] that:
 *  - attaches the Bearer token to every request
 *  - deserialises the Cloudflare envelope
 *  - maps every failure into a typed [CloudflareException]
 */
internal class CloudflareHttpClient(
    private val token: String,
    logLevel: LogLevel = LogLevel.NONE,
    httpClientOverride: HttpClient? = null,
) : AutoCloseable {

    val http: HttpClient = httpClientOverride ?: HttpClient(CIO) {
        install(ContentNegotiation) {
            json(cfJson)
        }
        install(Logging) {
            level  = logLevel
            logger = Logger.DEFAULT
        }
        install(HttpTimeout) {
            requestTimeoutMillis  = 30_000
            connectTimeoutMillis  = 10_000
            socketTimeoutMillis   = 30_000
        }
        // NOTE: We do NOT set url() here. Ktor merges defaultRequest base URL
        // with call-site paths using URL resolution rules — a path like
        // "/user/tokens/verify" would be treated as absolute, overwriting
        // the "/client/v4" prefix. Instead we call apiUrl(path) at each
        // call-site to build the full URL explicitly.
        defaultRequest {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
    }

    /** Execute a GET and unwrap the Cloudflare envelope into [T]. */
    suspend inline fun <reified T> get(
        path: String,
        queryParams: List<Pair<String, String>> = emptyList(),
    ): T {
        @Suppress("UNCHECKED_CAST")
        return getEnvelope<T>(path, queryParams).result as T
    }

    /** Execute a GET and return the full Cloudflare envelope (including result_info). */
    suspend inline fun <reified T> getEnvelope(
        path: String,
        queryParams: List<Pair<String, String>> = emptyList(),
    ): CloudflareResponse<T> = executeEnvelope {
        get(apiUrl(path)) {  // ← full URL here
            queryParams.forEach { (k, v) -> parameter(k, v) }
        }
    }

    // ── Writes ──────────────────────────────────────────────────────────────
    // Feature API classes (dns/api, email/api) build on these instead of editing
    // CloudflareClient, so each feature stays inside its own package.

    /** POST a JSON [body] and unwrap the envelope's result. */
    suspend inline fun <reified B, reified T> post(path: String, body: B): T {
        @Suppress("UNCHECKED_CAST")
        return executeEnvelope<T> { post(apiUrl(path)) { setBody(body) } }.result as T
    }

    /** PUT (full replace) a JSON [body] and unwrap the envelope's result. */
    suspend inline fun <reified B, reified T> put(path: String, body: B): T {
        @Suppress("UNCHECKED_CAST")
        return executeEnvelope<T> { put(apiUrl(path)) { setBody(body) } }.result as T
    }

    /** PATCH (partial update) a JSON [body] and unwrap the envelope's result. */
    suspend inline fun <reified B, reified T> patch(path: String, body: B): T {
        @Suppress("UNCHECKED_CAST")
        return executeEnvelope<T> { patch(apiUrl(path)) { setBody(body) } }.result as T
    }

    /** DELETE and unwrap the envelope's result (Cloudflare usually returns `{ "id": ... }`). */
    suspend inline fun <reified T> delete(path: String): T {
        @Suppress("UNCHECKED_CAST")
        return executeEnvelope<T> { delete(apiUrl(path)) }.result as T
    }

    /** Generic execute: wraps network + deserialisation errors, validates CF envelope. */
    suspend inline fun <reified T> executeEnvelope(
        crossinline block: suspend HttpClient.() -> HttpResponse,
    ): CloudflareResponse<T> {
        val response = try {
            http.block()
        } catch (e: IOException) {
            throw CloudflareException.NetworkError(e)
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText() }.getOrDefault("")
            // Cloudflare usually sends an error envelope even on 4xx; surface its errors if present.
            val errors = runCatching {
                cfJson.decodeFromString(CloudflareResponse.serializer(JsonElement.serializer()), body).errors
            }.getOrDefault(emptyList())
            if (errors.isNotEmpty()) {
                throw CloudflareException.ApiError(errors, response.status.value)
            }
            throw CloudflareException.HttpError(response.status.value, body)
        }

        val envelope = try {
            response.body<CloudflareResponse<T>>()
        } catch (e: Exception) {
            throw CloudflareException.DeserializationError(e)
        }

        if (!envelope.success) {
            throw CloudflareException.ApiError(envelope.errors, response.status.value)
        }

        return envelope
    }

    override fun close() = http.close()
}