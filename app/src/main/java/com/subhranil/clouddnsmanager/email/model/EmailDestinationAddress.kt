package com.subhranil.clouddnsmanager.email.model

import kotlinx.serialization.Serializable

/**
 * A destination ("forward to") address. Account-level in Cloudflare:
 * `GET /accounts/{account_id}/email/routing/addresses`.
 */
@Serializable
data class EmailDestinationAddress(
    val id: String = "",
    /** Deprecated duplicate of [id]. */
    val tag: String? = null,
    val email: String = "",
    val created: String? = null,
    val modified: String? = null,
    /** When the owner clicked Cloudflare's verification link. Null means not verified yet. */
    val verified: String? = null,
) {
    val addressId: String get() = id.ifBlank { tag.orEmpty() }
    val isVerified: Boolean get() = !verified.isNullOrBlank()
}

/** Body for `POST /accounts/{account_id}/email/routing/addresses`. */
@Serializable
data class CreateAddressRequest(val email: String)

/** Cloudflare's `{ "id": ... }` result for deletes (the full object is sometimes returned too). */
@Serializable
data class EmailDeleteResult(val id: String? = null)
