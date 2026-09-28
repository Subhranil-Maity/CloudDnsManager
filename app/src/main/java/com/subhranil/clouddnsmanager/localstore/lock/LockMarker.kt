package com.subhranil.clouddnsmanager.localstore.lock

/**
 * Text marker used to mirror a lock into a Cloudflare text field (e.g. a DNS record's
 * `comment`), so the lock survives reinstalls, shows on other devices and is visible in
 * the Cloudflare dashboard.
 */
object LockMarker {
    const val TOKEN = "[cfdm-locked]"

    fun isLocked(text: String?): Boolean = text?.contains(TOKEN) == true

    /**
     * Returns [text] with the marker added. If a [maxLength] is given (Cloudflare limits
     * comment length per plan), the existing text is shortened so the marker always fits.
     */
    fun add(text: String?, maxLength: Int? = null): String {
        val base = remove(text)
        if (base.isEmpty()) return TOKEN
        val withMarker = "$TOKEN $base"
        if (maxLength == null || withMarker.length <= maxLength) return withMarker
        val room = maxLength - TOKEN.length - 1
        return if (room <= 0) TOKEN else "$TOKEN ${base.take(room).trimEnd()}"
    }

    /** Returns [text] with the marker (and the space after it) removed. */
    fun remove(text: String?): String =
        text.orEmpty().replace("$TOKEN ", "").replace(TOKEN, "").trim()
}
