package com.subhranil.clouddnsmanager

/** Cloudflare error codes meaning the token is invalid or lacks a permission. */
private val AUTH_ERROR_CODES = setOf(9103, 9106, 9109, 10000, 10001)

/** "This record is managed by Email Routing. Disable Email Routing to modify/remove this record." */
const val CF_ERROR_MANAGED_BY_EMAIL_ROUTING = 1046

/** True when Cloudflare refused the call because of the token (missing permission or resource). */
fun Throwable.isPermissionError(): Boolean = when (this) {
    is CloudflareException.ApiError ->
        httpStatus == 401 || httpStatus == 403 || errors.any { it.code in AUTH_ERROR_CODES }
    is CloudflareException.HttpError -> status == 401 || status == 403
    else -> false
}

/**
 * Turns any failure from the Cloudflare API into a short message for the UI.
 *
 * @param permissionHint the token permission the failed call needs (e.g. "DNS: Edit"),
 *   included in the message when Cloudflare says the token isn't allowed.
 */
fun Throwable.toUserMessage(permissionHint: String? = null): String = when (this) {
    is CloudflareException.ApiError -> when {
        errors.any { it.code == CF_ERROR_MANAGED_BY_EMAIL_ROUTING } ->
            "This record is managed by Cloudflare Email Routing and can't be changed here."
        httpStatus == 401 || httpStatus == 403 || errors.any { it.code in AUTH_ERROR_CODES } ->
            permissionMessage(permissionHint, cloudflareDetail())
        else -> errors.firstOrNull()?.message ?: message ?: "Cloudflare returned an error."
    }
    is CloudflareException.HttpError ->
        if (status == 401 || status == 403) permissionMessage(permissionHint, "HTTP $status")
        else "Cloudflare returned HTTP $status."
    is CloudflareException.NetworkError -> "Network error. Check your connection and try again."
    is CloudflareException.DeserializationError -> "Cloudflare sent a response the app couldn't read."
    else -> message ?: "Something went wrong."
}

/** Cloudflare's own error codes and messages, e.g. "10000: Authentication error". */
private fun CloudflareException.ApiError.cloudflareDetail(): String =
    errors.joinToString("; ") { "${it.code}: ${it.message}" }.ifBlank { "HTTP $httpStatus" }

/**
 * The permission hint is only a guess based on what the screen was doing, so Cloudflare's
 * actual answer is always appended: it tells an invalid or outdated token apart from a
 * missing permission or a zone the token doesn't cover.
 */
private fun permissionMessage(permissionHint: String?, cloudflareDetail: String): String {
    val advice = if (permissionHint != null) {
        "Your API token was refused. It may be missing the \"$permissionHint\" permission, or not include this zone/account in its resources."
    } else {
        "Your API token was refused. Check the token's permissions and resources in the Cloudflare dashboard."
    }
    return "$advice\n\nCloudflare said: $cloudflareDetail"
}
