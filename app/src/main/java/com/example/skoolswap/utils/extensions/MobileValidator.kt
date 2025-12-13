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
        val validPrefixes = setOf(
            // Vodacom
            "060", "061", "062", "063", "064",
            // MTN
            "071", "072", "073", "074", "076",
            // Cell C
            "079", "078",
            // Telkom, Virgin Mobile, etc.
            "081", "082", "083", "084", "085"
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
            !cleanMobile.startsWith("0") -> "Mobile number must start with 0"
            cleanMobile.length != 10 -> "Mobile number must be 10 digits"
            !cleanMobile.all { it.isDigit() } -> "Mobile number must contain only digits"
            !isValidSouthAfricanMobile(cleanMobile) -> "Invalid South African mobile number"
            else -> null
        }
    }
}