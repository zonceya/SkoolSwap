package com.example.skoolswap.common.constants

// Change from class to object
object AppConstants {
    const val BASE_URL = "https://api.sekeni.xyz/"
    const val API_TIMEOUT = 30L

    // Database Constants
    const val DATABASE_NAME = "skoolswap_database"
    const val DATABASE_VERSION = 1

    // SharedPreferences Keys
    const val PREF_NAME = "skoolswap_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"

    // Navigation Constants
    const val ARG_USER_ID = "userId"
    const val ARG_SHOP_ID = "shopId"

    // Request Codes
    const val RC_GOOGLE_SIGN_IN = 1001
    const val RC_IMAGE_PICK = 1002

    // Login Constants
    const val GOOGLE = "google"  // Changed to uppercase (convention)
    const val FACEBOOK = "facebook"
}