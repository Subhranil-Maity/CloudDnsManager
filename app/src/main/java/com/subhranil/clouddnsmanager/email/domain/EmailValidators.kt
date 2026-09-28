package com.subhranil.clouddnsmanager.email.domain

/** Result of validating user input: null error means valid. */
data class ValidationResult(val value: String, val error: String?) {
    val isValid: Boolean get() = error == null
}

/**
 * Pure validators for the email forms. Deliberately a practical subset of RFC 5321/5322:
 * the unquoted "dot-atom" local parts Cloudflare accepts for routing rules.
 */
object EmailValidators {

    /** RFC 5321 limit for the part before "@". */
    const val MAX_LOCAL_PART = 64

    /** Cloudflare rejects matcher values and addresses longer than this. */
    const val MAX_ADDRESS = 90

    private val LOCAL_PART_CHARS = Regex("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+$")
    private val DOMAIN = Regex("^(?=.{1,253}$)([A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$")

    /** Validates an alias's local part (what goes before `@zone`). Input is trimmed and lowercased. */
    fun validateLocalPart(input: String, zoneName: String? = null): ValidationResult {
        val value = input.trim().lowercase()
        val error = when {
            value.isEmpty() -> "Enter the part before the @."
            value.contains('@') -> "Enter only the part before the @."
            value.any { it.isWhitespace() } -> "Spaces aren't allowed."
            value.length > MAX_LOCAL_PART -> "Use at most $MAX_LOCAL_PART characters."
            !LOCAL_PART_CHARS.matches(value) -> "Use letters, numbers, dots, dashes or underscores."
            value.startsWith('.') || value.endsWith('.') -> "It can't start or end with a dot."
            value.contains("..") -> "It can't contain two dots in a row."
            zoneName != null && "$value@$zoneName".length > MAX_ADDRESS ->
                "The full address must be at most $MAX_ADDRESS characters."
            else -> null
        }
        return ValidationResult(value, error)
    }

    /** Validates a full email address (e.g. a new destination address). Input is trimmed. */
    fun validateEmail(input: String): ValidationResult {
        val value = input.trim()
        val at = value.lastIndexOf('@')
        val error = when {
            value.isEmpty() -> "Enter an email address."
            at <= 0 || at == value.length - 1 -> "Enter a full email address, like name@example.com."
            value.length > MAX_ADDRESS -> "Use at most $MAX_ADDRESS characters."
            validateLocalPart(value.substring(0, at)).error != null -> "The part before the @ isn't valid."
            !DOMAIN.matches(value.substring(at + 1)) -> "The domain after the @ isn't valid."
            else -> null
        }
        return ValidationResult(value, error)
    }
}
