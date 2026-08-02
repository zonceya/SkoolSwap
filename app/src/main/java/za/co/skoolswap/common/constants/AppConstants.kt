package za.co.skoolswap.common.constants

object AppConstants {
    // ============ BASE CONFIGURATION ============
    const val BASE_URL = "https://api.skoolswap.co.za/"
    const val API_VERSION = "v1"
    const val FULL_BASE_URL = "$BASE_URL$API_VERSION/"
    const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hour
    // ============ TIMEOUTS ============
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    const val API_TIMEOUT = 30L
    const val TIMEOUT = 1000L
    const val SUPPORT_EMAIL = "admin@skoolswap.com"
    // ============ DATABASE ============
    const val DATABASE_NAME = "skoolswap_database"
    const val DATABASE_VERSION = 1

    // ============ SHARED PREFERENCES KEYS ============
    const val PREF_NAME = "skoolswap_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_PROFILE_IMAGE = "user_profile_image"
    const val KEY_HAS_GOOGLE_ACCOUNTS = "has_google_accounts"

    // ============ NAVIGATION ARGUMENTS ============
    const val ARG_USER_ID = "userId"
    const val ARG_SHOP_ID = "shopId"
    const val ARG_PRODUCT_ID = "productId"
    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_SUBCATEGORY_ID = "subcategoryId"
    const val ARG_SCHOOL_ID = "schoolId"
    // ============ REQUEST CODES ============
    const val RC_GOOGLE_SIGN_IN = 1001
    const val RC_IMAGE_PICK = 1002
    const val RC_PERMISSION_REQUEST = 1003
    const val RC_CAMERA = 1004
    const val RC_GALLERY = 1005

    // ============ AUTH MODES ============
    const val GOOGLE = "google"
    const val EMAIL = "email"
    const val FACEBOOK = "facebook"

    // ============ DEFAULT VALUES ============
    const val DEFAULT_USER_NAME = "User"
    const val EMPTY_STRING = ""
    const val DEFAULT_PAGE_SIZE = 20
    const val DEFAULT_PAGE_NUMBER = 1

    // ============ LOG TAGS ============
    object LogTags {
        const val AUTH = "AuthRepository"
        const val NETWORK = "Network"
        const val DATABASE = "Database"
        const val UI = "UI"
        const val REPOSITORY = "Repository"
        const val VIEW_MODEL = "ViewModel"
        const val FRAGMENT = "Fragment"
        const val ACTIVITY = "Activity"
        const val SERVICE = "Service"
    }

    // ============ SYNC STATUS ============
    object SyncStatus {
        const val ACTIVE = "ACTIVE"
        const val UPLOADING = "UPLOADING"
        const val UPDATING = "UPDATING"
        const val UPDATE_FAILED = "UPDATE_FAILED"
        const val SYNCED = "SYNCED"
        const val FAILED = "FAILED"
        const val PENDING = "PENDING"
        const val DELETED = "DELETED"
    }

    // ============ REGEX PATTERNS ============
    object Patterns {
        const val EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        const val PHONE = "^[0-9]{10,15}$"
        const val PASSWORD = "^.{6,}$"
        const val NAME = "^[A-Za-z\\s]{2,50}$"
    }
}