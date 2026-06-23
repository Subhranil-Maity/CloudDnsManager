package com.subhranil.clouddnsmanager

import com.subhranil.clouddnsmanager.models.CloudflareError

/**
 * Sealed hierarchy so callers can exhaustively handle every failure mode.
 */
sealed class CloudflareException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** The API returned success=false with Cloudflare error objects. */
    class ApiError(
        val errors: List<CloudflareError>,
        val httpStatus: Int,
    ) : CloudflareException(
        "Cloudflare API error (HTTP $httpStatus): ${errors.joinToString { "[${it.code}] ${it.message}" }}"
    )

    /** HTTP transport succeeded but the response body was unexpected. */
    class DeserializationError(cause: Throwable) :
        CloudflareException("Failed to deserialise Cloudflare response", cause)

    /** Non-2xx HTTP response with no Cloudflare error body. */
    class HttpError(val status: Int, body: String) :
        CloudflareException("HTTP $status: $body")

    /** Network-level failure (timeout, DNS, TLS, …). */
    class NetworkError(cause: Throwable) :
        CloudflareException("Network error: ${cause.message}", cause)
}