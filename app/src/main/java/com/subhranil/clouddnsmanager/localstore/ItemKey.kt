package com.subhranil.clouddnsmanager.localstore

/**
 * Stable identity of anything that can carry a local note or a lock.
 * Always build keys with the factory functions so both features use the same format.
 */
@JvmInline
value class ItemKey(val value: String) {
    companion object {
        fun dnsRecord(zoneId: String, recordId: String) = ItemKey("dns/$zoneId/$recordId")
        fun dnsZonePrefix(zoneId: String) = "dns/$zoneId/"

        /** An email routing rule (alias) in a zone. */
        fun emailRule(zoneId: String, ruleId: String) = ItemKey("email-rule/$zoneId/$ruleId")
        fun emailRulePrefix(zoneId: String) = "email-rule/$zoneId/"

        /** A destination (forward-to) address, which is account-level in Cloudflare. */
        fun emailAddress(accountId: String, addressId: String) = ItemKey("email-address/$accountId/$addressId")
        fun emailAddressPrefix(accountId: String) = "email-address/$accountId/"
    }
}
