package com.example.skoolswap.common.constants

object ErrorConstants {
    // ============ HTTP STATUS CODES ============
    object HttpStatus {
        const val OK = 200
        const val CREATED = 201
        const val ACCEPTED = 202
        const val NO_CONTENT = 204

        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val PAYMENT_REQUIRED = 402
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val METHOD_NOT_ALLOWED = 405
        const val CONFLICT = 409
        const val UNPROCESSABLE_ENTITY = 422
        const val TOO_MANY_REQUESTS = 429

        const val INTERNAL_SERVER = 500
        const val NOT_IMPLEMENTED = 501
        const val BAD_GATEWAY = 502
        const val SERVICE_UNAVAILABLE = 503
        const val GATEWAY_TIMEOUT = 504

        // Custom
        const val AUTHENTICATION_TIMEOUT = 530
        const val NETWORK_TIMEOUT = 599
    }

    // ============ ERROR MODELS ============
    sealed class AppError(
        open val code: String,
        open val userMessage: String,
        open val technicalMessage: String? = null,
        open val isRetryable: Boolean = true,
        open val action: ErrorAction? = null
    ) {
        // Network Errors
        data class NetworkError(
            override val code: String = "NETWORK_ERROR",
            override val userMessage: String = Messages.UserFriendly.NO_INTERNET,
            override val technicalMessage: String? = null,
            override val isRetryable: Boolean = true,
            override val action: ErrorAction? = ErrorAction.RETRY
        ) : AppError(code, userMessage, technicalMessage, isRetryable, action)

        // Authentication Errors
        data class AuthError(
            override val code: String = "AUTH_ERROR",
            override val userMessage: String = Messages.UserFriendly.SESSION_EXPIRED,
            override val technicalMessage: String? = null,
            override val isRetryable: Boolean = false,
            override val action: ErrorAction? = ErrorAction.SIGN_OUT
        ) : AppError(code, userMessage, technicalMessage, isRetryable, action)

        // Validation Errors
        data class ValidationError(
            override val code: String = "VALIDATION_ERROR",
            override val userMessage: String = Messages.UserFriendly.INVALID_INPUT,
            override val technicalMessage: String? = null,
            override val isRetryable: Boolean = false,
            override val action: ErrorAction? = ErrorAction.RETRY
        ) : AppError(code, userMessage, technicalMessage, isRetryable, action)

        // Server Errors
        data class ServerError(
            override val code: String = "SERVER_ERROR",
            override val userMessage: String = Messages.UserFriendly.SERVER_DOWN,
            override val technicalMessage: String? = null,
            override val isRetryable: Boolean = true,
            override val action: ErrorAction? = ErrorAction.RETRY
        ) : AppError(code, userMessage, technicalMessage, isRetryable, action)

        // Unknown Errors
        data class UnknownError(
            override val code: String = "UNKNOWN_ERROR",
            override val userMessage: String = Messages.UserFriendly.UNKNOWN,
            override val technicalMessage: String? = null,
            override val isRetryable: Boolean = true,
            override val action: ErrorAction? = ErrorAction.RETRY
        ) : AppError(code, userMessage, technicalMessage, isRetryable, action)

        // Timeout Errors
        data class TimeoutError(
            override val code: String = "TIMEOUT_ERROR",
            override val userMessage: String = Messages.UserFriendly.TIMEOUT,
            override val technicalMessage: String? = null,
            override val isRetryable: Boolean = true,
            override val action: ErrorAction? = ErrorAction.RETRY
        ) : AppError(code, userMessage, technicalMessage, isRetryable, action)
    }

    // ============ ERROR ACTIONS ============
    enum class ErrorAction {
        RETRY,
        SIGN_OUT,
        GO_TO_SETTINGS,
        CONTACT_SUPPORT,
        CLEAR_CACHE,
        UPDATE_APP,
        GO_TO_LOGIN,
        SHOW_DIALOG,
        NO_ACTION
    }

    // ============ USER-FRIENDLY MESSAGES ============
    object Messages {
        object UserFriendly {
            // Network
            const val NO_INTERNET = "📡 No Internet Connection. Please check your Wi-Fi or mobile data."
            const val SLOW_CONNECTION = "⚠️ Your connection is slow. Please try again."
            const val TIMEOUT = "⏱️ Connection timeout. Please try again."

            // Server
            const val SERVER_DOWN = "🔧 Our servers are experiencing issues. Please try again later."
            const val SERVER_MAINTENANCE = "🛠️ We're currently performing maintenance. Please check back later."
            const val SERVER_BUSY = "📊 Server is busy. Please try again in a few minutes."

            // Authentication
            const val SESSION_EXPIRED = "🔒 Your session has expired. Please login again."
            const val INVALID_CREDENTIALS = "❌ Invalid email or password. Please try again."
            const val ACCOUNT_LOCKED = "🔒 Your account has been locked. Please contact support."
            const val EMAIL_NOT_VERIFIED = "📧 Please verify your email before logging in."
            const val ACCOUNT_DELETED = "🗑️ This account has been deleted."
            const val ACCOUNT_DISABLED = "⛔ This account has been disabled."
            const val SIGN_IN_CANCELLED = "Sign-in was cancelled by user."  // ✅ ADD THIS

            // Validation
            const val INVALID_INPUT = "📝 Please check your input and try again."
            const val INVALID_EMAIL = "📧 Please enter a valid email address."
            const val INVALID_PHONE = "📱 Please enter a valid phone number."
            const val INVALID_PASSWORD = "🔑 Password must be at least 6 characters."
            const val INVALID_NAME = "👤 Please enter a valid name."

            // Resource
            const val NOT_FOUND = "🔍 The requested resource was not found."
            const val ALREADY_EXISTS = "📁 This item already exists."
            const val OUT_OF_STOCK = "📦 This item is currently out of stock."

            // Permission
            const val PERMISSION_DENIED = "🚫 You don't have permission to perform this action."
            const val ACCESS_DENIED = "🚫 Access denied."

            // Generic
            const val UNKNOWN = "❓ Something went wrong. Please try again."
            const val RETRY = "🔄 Please try again."
            const val CONTACT_SUPPORT = "📞 Please contact support if the issue persists."
        }

        object Technical {
            const val JSON_PARSE = "Failed to parse JSON response"
            const val SSL_ERROR = "SSL/TLS handshake failed"
            const val DNS_FAILURE = "DNS resolution failed"
            const val TOKEN_EXPIRED = "JWT token expired"
            const val TOKEN_INVALID = "Invalid JWT token"
            const val TOKEN_MALFORMED = "Malformed JWT token"
            const val NETWORK_UNREACHABLE = "Network unreachable"
            const val CONNECTION_REFUSED = "Connection refused"
            const val HOST_UNREACHABLE = "Host unreachable"
            const val SOCKET_TIMEOUT = "Socket timeout"
            const val IO_ERROR = "I/O error occurred"
            const val DATABASE_ERROR = "Database error"
            const val CACHE_ERROR = "Cache error"
        }
    }
}