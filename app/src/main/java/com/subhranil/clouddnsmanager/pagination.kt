package com.subhranil.clouddnsmanager


import com.subhranil.clouddnsmanager.models.ResultInfo

/** Represents a single page of results with its metadata. */
data class Page<T>(
    val items: List<T>,
    val info: ResultInfo?,
) {
    val hasNextPage: Boolean
        get() = info != null && info.page < info.totalPages
}

/** Parameters shared by all paginated list endpoints. */
data class PaginationParams(
    val page: Int = 1,
    val perPage: Int = 50,
) {
    init {
        require(page >= 1)       { "page must be >= 1" }
        require(perPage in 1..1000) { "perPage must be 1..1000" }
    }

    internal fun toQueryParams(): List<Pair<String, String>> = listOf(
        "page"     to page.toString(),
        "per_page" to perPage.toString(),
    )
}

/** Zone list filter options. */
data class ZoneFilter(
    val name: String? = null,
    val status: String? = null,    // "active" | "pending" | "paused"
    val accountId: String? = null,
    val accountName: String? = null,
) {
    internal fun toQueryParams(): List<Pair<String, String>> = buildList {
        name?.let        { add("name"         to it) }
        status?.let      { add("status"       to it) }
        accountId?.let   { add("account.id"   to it) }
        accountName?.let { add("account.name" to it) }
    }
}

/** DNS record list filter options. */
data class DnsRecordFilter(
    val type: String? = null,      // "A" | "AAAA" | "CNAME" | …
    val name: String? = null,
    val content: String? = null,
    val proxied: Boolean? = null,
) {
    internal fun toQueryParams(): List<Pair<String, String>> = buildList {
        type?.let    { add("type"    to it) }
        name?.let    { add("name"    to it) }
        content?.let { add("content" to it) }
        proxied?.let { add("proxied" to it.toString()) }
    }
}