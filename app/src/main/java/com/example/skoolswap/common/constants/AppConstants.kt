package com.example.skoolswap.common.constants

object AppConstants {
    const val BASE_URL = "https://api.sekeni.xyz/"
    const val API_TIMEOUT = 30L
    const val TIMEOUT = 1000L
    // Database Constants
    const val DATABASE_NAME = "skoolswap_database"
    const val DATABASE_VERSION = 1

    // SharedPreferences Keys
    const val PREF_NAME = "skoolswap_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_PROFILE_IMAGE = "user_profile_image"
    const val KEY_HAS_GOOGLE_ACCOUNTS = "has_google_accounts"

    // Navigation Constants
    const val ARG_USER_ID = "userId"
    const val ARG_SHOP_ID = "shopId"
    const val ARG_PRODUCT_ID = "productId"

    // Request Codes
    const val RC_GOOGLE_SIGN_IN = 1001
    const val RC_IMAGE_PICK = 1002
    const val RC_PERMISSION_REQUEST = 1003

    // Auth Modes
    const val GOOGLE = "google"
    const val EMAIL = "email"
    const val FACEBOOK = "facebook"

    // Default Values
    const val DEFAULT_USER_NAME = "User"
    const val EMPTY_STRING = ""

    // Log Tags
    object LogTags {
        const val AUTH = "AuthRepository"
        const val NETWORK = "Network"
        const val DATABASE = "Database"
        const val UI = "UI"
    }
}