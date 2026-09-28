package com.subhranil.clouddnsmanager.dns.record

/** Human label for a TTL in seconds; 1 means Auto on Cloudflare. */
fun ttlLabel(ttl: Int): String = when {
    ttl == 1 -> "Auto"
    ttl % 86400 == 0 -> (ttl / 86400).let { if (it == 1) "1 day" else "$it days" }
    ttl % 3600 == 0 -> (ttl / 3600).let { if (it == 1) "1 hour" else "$it hours" }
    ttl % 60 == 0 -> (ttl / 60).let { if (it == 1) "1 min" else "$it min" }
    else -> "$ttl seconds"
}
