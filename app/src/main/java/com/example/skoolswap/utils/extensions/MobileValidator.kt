package com.example.skoolswap.utils.extensions

object MobileValidator {

    /**
     * Validates South African mobile numbers
     * Requirements:
     * - Must start with 0
     * - Must be exactly 10 digits
     * - Must contain only digits
     * - Must have a valid SA prefix
     */
    fun isValidSouthAfricanMobile(mobile: String): Boolean {
        // Clean the input
        val cleanMobile = mobile.trim()

        // Basic validation
        if (cleanMobile.isEmpty()) return false
        if (!cleanMobile.startsWith("0")) return false
        if (cleanMobile.length != 10) return false
        if (!cleanMobile.all { it.isDigit() }) return false

        // Validate South African mobile prefixes
        val prefix = cleanMobile.substring(0, 3)

        // Comprehensive list of valid SA mobile prefixes
        val validPrefixes = setOf(
            // Vodacom (060-069)
            "060", "061", "062", "063", "064", "065", "066", "067", "068", "069",

            // MTN (071-076, 078, 083)
            "071", "072", "073", "074", "075", "076", "078", "083",

            // Cell C (079, 084)
            "079", "084",

            // Telkom (081, 082, 085)
            "081", "082", "085",

            // Other/Virtual Networks
            "080", "086", "087", "088", "089"
        )

        return validPrefixes.contains(prefix)
    }

    /**
     * Formats mobile number to international format
     * Example: 0712345678 → +27712345678
     */
    fun formatToInternational(mobile: String): String {
        return if (mobile.length == 10 && mobile.startsWith("0")) {
            "+27${mobile.substring(1)}"
        } else {
            mobile
        }
    }

    /**
     * Formats mobile number to local format
     * Example: +27712345678 → 0712345678
     */
    fun formatToLocal(mobile: String): String {
        return if (mobile.startsWith("+27") && mobile.length == 12) {
            "0${mobile.substring(3)}"
        } else {
            mobile
        }
    }

    /**
     * Gets error message for invalid mobile
     */
    fun getErrorMessage(mobile: String): String? {
        val cleanMobile = mobile.trim()

        return when {
            cleanMobile.isEmpty() -> "Mobile number is required"
            !cleanMobile.startsWith("0") -> "Must start with 0 (e.g., 0712345678)"
            cleanMobile.length < 10 -> "Mobile number must be 10 digits (currently ${cleanMobile.length})"
            cleanMobile.length > 10 -> "Mobile number must be 10 digits (currently ${cleanMobile.length})"
            !cleanMobile.all { it.isDigit() } -> "Mobile number must contain only digits"
            !isValidSouthAfricanMobile(cleanMobile) -> {
                val prefix = cleanMobile.take(3)
                "Invalid SA mobile number. '$prefix' is not a valid prefix. Use 060-089"
            }
            else -> null
        }
    }

    /**
     * Gets user-friendly error message for UI
     */
    fun getUserFriendlyErrorMessage(mobile: String): String? {
        val cleanMobile = mobile.trim()

        return when {
            cleanMobile.isEmpty() -> "📱 Please enter your mobile number"
            !cleanMobile.startsWith("0") -> "📱 South African numbers start with 0\nExample: 0712345678"
            cleanMobile.length < 10 -> "📱 Need ${10 - cleanMobile.length} more digit(s)"
            cleanMobile.length > 10 -> "📱 Too many digits (${cleanMobile.length - 10} extra)"
            !cleanMobile.all { it.isDigit() } -> "📱 Numbers only, no spaces or dashes"
            !isValidSouthAfricanMobile(cleanMobile) -> {
                val prefix = cleanMobile.take(3)
                "📱 '$prefix' is not a valid SA prefix\nValid: 060-089"
            }
            else -> null
        }
    }
}