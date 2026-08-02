package za.co.skoolswap.common.constants

object NetworkConstants {
    // ============ NETWORK CONFIGURATION ============
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    // ============ CACHE ============
    const val CACHE_SIZE = 10 * 1024 * 1024 // 10 MB
    const val CACHE_MAX_AGE = 60 // 60 seconds
    const val CACHE_MAX_STALE = 60 * 60 * 24 * 7 // 7 days

    // ============ RETRY ============
    const val MAX_RETRY_COUNT = 3
    const val RETRY_DELAY_MS = 1000L
    const val RETRY_BACKOFF_MULTIPLIER = 2.0

    // ============ API ENDPOINTS ============
    object Endpoints {
        private const val API = "api"
        private const val V1 = "v1"
        private const val BASE_PATH = "/$API/$V1"

        // ===== Auth Endpoints =====
        object Auth {
            private const val AUTH = "$BASE_PATH/auth"
            const val REFRESH_TOKEN = "$AUTH/refresh"
            const val LOGOUT = "$AUTH/logout"
            const val VALIDATE_TOKEN = "$AUTH/validate"
        }

        // ===== User Endpoints =====
        object User {
            private const val USERS = "$BASE_PATH/users"

            // Authentication
            const val FIREBASE_AUTH = "$USERS/firebase_auth"
            const val SIGN_IN = "$USERS/sign_in"
            const val SIGN_UP = "$USERS/signup"

            // OTP
            const val SEND_LOGIN_OTP = "$USERS/send_login_otp"
            const val SEND_SIGNUP_OTP = "$USERS/send_signup_otp"
            const val VERIFY_SIGNUP = "$USERS/verify_signup"
            const val VERIFY_LOGIN = "$USERS/verify_login_otp"
            const val RESEND_SIGNUP_OTP = "$USERS/resend_signup_otp"
            const val RESEND_LOGIN_OTP = "$USERS/resend_login_otp"

            // Profile
            const val PROFILE = "$USERS/profile"
            const val UPDATE_PROFILE = "$USERS/profile"
            const val UPDATE_MOBILE = "$USERS/update_mobile"
            const val UPDATE_EMAIL = "$USERS/update_email"
            const val CHANGE_PASSWORD = "$USERS/change_password"

            // Account Management
            const val DISABLE_USER = "$USERS/disable"
            const val DELETE_USER = "$USERS/delete"
            const val REACTIVATE_USER = "$USERS/reactivate"

            // User Data
            const val GET_USER = "$USERS/{userId}"
            const val GET_USER_BY_EMAIL = "$USERS/by_email"
            const val SEARCH_USERS = "$USERS/search"

            // School
            const val ASSIGN_SCHOOL = "$USERS/assign_school"
            const val VERIFY_SCHOOL = "$USERS/verify_school"
        }

        // ===== Shop Endpoints =====
        object Shop {
            private const val SHOPS = "$BASE_PATH/shops"

            // CRUD
            const val GET_SHOPS = SHOPS
            const val GET_SHOP = "$SHOPS/{shopId}"
            const val CREATE_SHOP = SHOPS
            const val UPDATE_SHOP = "$SHOPS/{shopId}"
            const val DELETE_SHOP = "$SHOPS/{shopId}"

            // Shop Operations
            const val GET_SHOP_PRODUCTS = "$SHOPS/{shopId}/products"
            const val GET_SHOP_ORDERS = "$SHOPS/{shopId}/orders"
            const val GET_SHOP_REVIEWS = "$SHOPS/{shopId}/reviews"
            const val UPDATE_SHOP_STATUS = "$SHOPS/{shopId}/status"

            // Shop Analytics
            const val SHOP_ANALYTICS = "$SHOPS/{shopId}/analytics"
            const val SHOP_STATS = "$SHOPS/{shopId}/stats"
        }

        // ===== Product Endpoints =====
        object Product {
            private const val PRODUCTS = "$BASE_PATH/products"

            // CRUD
            const val GET_PRODUCTS = PRODUCTS
            const val GET_PRODUCT = "$PRODUCTS/{productId}"
            const val CREATE_PRODUCT = PRODUCTS
            const val UPDATE_PRODUCT = "$PRODUCTS/{productId}"
            const val DELETE_PRODUCT = "$PRODUCTS/{productId}"

            // Product Operations
            const val SEARCH_PRODUCTS = "$PRODUCTS/search"
            const val FILTER_PRODUCTS = "$PRODUCTS/filter"
            const val GET_PRODUCT_CATEGORIES = "$PRODUCTS/categories"
            const val GET_PRODUCT_IMAGES = "$PRODUCTS/{productId}/images"
            const val UPLOAD_PRODUCT_IMAGE = "$PRODUCTS/{productId}/images"
            const val DELETE_PRODUCT_IMAGE = "$PRODUCTS/{productId}/images/{imageId}"

            // Favorites
            const val GET_FAVORITES = "$PRODUCTS/favorites"
            const val ADD_FAVORITE = "$PRODUCTS/{productId}/favorite"
            const val REMOVE_FAVORITE = "$PRODUCTS/{productId}/favorite"

            // Reviews
            const val GET_PRODUCT_REVIEWS = "$PRODUCTS/{productId}/reviews"
            const val CREATE_PRODUCT_REVIEW = "$PRODUCTS/{productId}/reviews"
        }

        // ===== Order Endpoints =====
        object Order {
            private const val ORDERS = "$BASE_PATH/orders"

            const val GET_ORDERS = ORDERS
            const val GET_ORDER = "$ORDERS/{orderId}"
            const val CREATE_ORDER = ORDERS
            const val UPDATE_ORDER = "$ORDERS/{orderId}"
            const val CANCEL_ORDER = "$ORDERS/{orderId}/cancel"
            const val CONFIRM_ORDER = "$ORDERS/{orderId}/confirm"

            // Order Tracking
            const val ORDER_STATUS = "$ORDERS/{orderId}/status"
            const val ORDER_TRACKING = "$ORDERS/{orderId}/tracking"
        }

        // ===== Payment Endpoints =====
        object Payment {
            private const val PAYMENTS = "$BASE_PATH/payments"

            const val INITIATE_PAYMENT = "$PAYMENTS/initiate"
            const val CONFIRM_PAYMENT = "$PAYMENTS/confirm"
            const val GET_PAYMENT_STATUS = "$PAYMENTS/{paymentId}/status"
            const val GET_PAYMENT_HISTORY = "$PAYMENTS/history"
        }

        // ===== School Endpoints =====
        object School {
            private const val SCHOOLS = "$BASE_PATH/schools"

            const val GET_SCHOOLS = SCHOOLS
            const val GET_SCHOOL = "$SCHOOLS/{schoolId}"
            const val SEARCH_SCHOOLS = "$SCHOOLS/search"
            const val GET_SCHOOLS_BY_PROVINCE = "$SCHOOLS/province/{provinceId}"
            const val VERIFY_SCHOOL = "$SCHOOLS/verify"
        }

        // ===== Category Endpoints =====
        object Category {
            private const val CATEGORIES = "$BASE_PATH/categories"

            const val GET_CATEGORIES = CATEGORIES
            const val GET_CATEGORY = "$CATEGORIES/{categoryId}"
            const val GET_SUBCATEGORIES = "$CATEGORIES/{categoryId}/subcategories"
            const val GET_CATEGORY_ITEMS = "$CATEGORIES/{categoryId}/items"
        }
    }

    // ============ HTTP HEADERS ============
    object Headers {
        const val AUTHORIZATION = "Authorization"
        const val CONTENT_TYPE = "Content-Type"
        const val ACCEPT = "Accept"
        const val USER_AGENT = "User-Agent"

        const val APPLICATION_JSON = "application/json"
        const val MULTIPART_FORM_DATA = "multipart/form-data"
        const val BEARER = "Bearer"
    }
}