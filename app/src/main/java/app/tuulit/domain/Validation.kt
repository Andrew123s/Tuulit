package app.tuulit.domain

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Input validation shared by forms. Each function returns an error message, or null when valid. */
object Validation {
    private val emailRegex = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")

    fun email(value: String): String? = when {
        value.isBlank() -> "Enter your email address."
        !emailRegex.matches(value.trim()) -> "That doesn't look like an email address."
        else -> null
    }

    fun password(value: String): String? = when {
        value.length < 10 -> "Use at least 10 characters."
        value.none { it.isLetter() } || value.none { it.isDigit() } -> "Mix letters and at least one number."
        else -> null
    }

    fun pin(value: String): String? = when {
        value.length !in 4..6 -> "Use 4 to 6 digits."
        !value.all { it.isDigit() } -> "Digits only."
        value.toSet().size == 1 -> "Avoid using the same digit throughout."
        else -> null
    }

    fun displayName(value: String): String? = when {
        value.length > 40 -> "Keep it under 40 characters."
        else -> null
    }

    fun cycleLength(days: Int): String? = if (days in 15..60) null else "Cycle length must be between 15 and 60 days."
    fun periodLength(days: Int): String? = if (days in 1..14) null else "Period length must be between 1 and 14 days."

    fun inviteCode(value: String): String? = when {
        value.length != 6 -> "Codes have 6 characters, like 4K8P2Q."
        !value.all { it.isLetterOrDigit() } -> "Letters and numbers only."
        else -> null
    }

    fun shortText(value: String, max: Int = 120): String? = when {
        value.isBlank() -> "This can't be empty."
        value.length > max -> "Keep it under $max characters."
        else -> null
    }
}

/** PBKDF2 hashing for the app-lock PIN. The PIN itself is never stored. */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun hash(pin: String, salt: ByteArray = newSalt()): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val enc = Base64.getEncoder()
        return "pbkdf2:$ITERATIONS:${enc.encodeToString(salt)}:${enc.encodeToString(key)}"
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val dec = Base64.getDecoder()
        val salt = dec.decode(parts[2])
        val spec = PBEKeySpec(pin.toCharArray(), salt, parts[1].toInt(), KEY_BITS)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return java.security.MessageDigest.isEqual(key, dec.decode(parts[3]))
    }

    private fun newSalt() = ByteArray(16).also { SecureRandom().nextBytes(it) }
}
