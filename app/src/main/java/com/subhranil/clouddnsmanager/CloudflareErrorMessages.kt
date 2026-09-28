package com.subhranil.clouddnsmanager

/** Cloudflare error codes meaning the token is invalid or lacks a permission. */
private val AUTH_ERROR_CODES = setOf(9103, 9106, 9109, 10000, 10001)

/** "This record is managed by Email Routing. Disable Email Routing to modify/remove this record." */
const val CF_ERROR_MANAGED_BY_EMAIL_ROUTING = 1046

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
            permissionMessage(permissionHint)
        else -> errors.firstOrNull()?.message ?: message ?: "Cloudflare returned an error."
    }
    is CloudflareException.HttpError ->
        if (status == 401 || status == 403) permissionMessage(permissionHint) else "Cloudflare returned HTTP $status."
    is CloudflareException.NetworkError -> "Network error. Check your connection and try again."
    is CloudflareException.DeserializationError -> "Cloudflare sent a response the app couldn't read."
    else -> message ?: "Something went wrong."
}

private fun permissionMessage(permissionHint: String?): String =
    if (permissionHint != null) {
        "Your API token isn't allowed to do this. Add the \"$permissionHint\" permission to the token in the Cloudflare dashboard."
    } else {
        "Your API token isn't allowed to do this. Check the token's permissions in the Cloudflare dashboard."
    }
