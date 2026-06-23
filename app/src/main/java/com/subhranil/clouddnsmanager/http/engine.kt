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
    ): T = execute(queryParams) { get(apiUrl(path)) }  // ← full URL here

    /** Generic execute: wraps network + deserialisation errors, validates CF envelope. */
    suspend inline fun <reified T> execute(
        queryParams: List<Pair<String, String>> = emptyList(),
        crossinline block: suspend HttpClient.() -> HttpResponse,
    ): T {
        val response = try {
            http.block()
        } catch (e: IOException) {
            throw CloudflareException.NetworkError(e)
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText() }.getOrDefault("")
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

        @Suppress("UNCHECKED_CAST")
        return envelope.result as T
    }

    override fun close() = http.close()
}