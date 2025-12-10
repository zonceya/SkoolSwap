package com.example.skoolswap.common.constants

object ErrorConstants {
    // Authentication Errors
    object Auth {
        const val NO_GOOGLE_ACCOUNTS = "No Google accounts found on device. Please add one in Settings."
        const val SIGN_IN_CANCELLED = "Sign-in was cancelled by user"
        const val GOOGLE_SIGN_IN_FAILED = "Google sign-in failed: %s"
        const val INVALID_ID_TOKEN = "Invalid Google ID token response"
        const val UNEXPECTED_CREDENTIAL_TYPE = "Unexpected credential type"
        const val FIREBASE_USER_NULL = "Firebase user is null"
        const val ACTIVITY_CONTEXT_REQUIRED = "Google sign-in requires an Activity context"

        // Sign-in errors
        const val SIGN_IN_FAILED = "Sign-in failed: %s"
        const val BACKEND_SIGN_IN_FAILED = "Backend sign-in failed"
    }

    // Network Errors
    object Network {
        const val CLIENT_ERROR = "Client error: Please check your request"
        const val SERVER_ERROR = "Server error: Please try again later"
        const val NETWORK_ERROR = "Network error: Please check your connection"
        const val BAD_REQUEST = "Bad request: Please check your credentials"
        const val UNAUTHORIZED = "Unauthorized: Invalid credentials"
        const val FORBIDDEN = "Forbidden: Access denied"
        const val NOT_FOUND = "Not found: User not registered"
        const val INTERNAL_SERVER_ERROR = "Server error: Please try again later"
    }

    // Database Errors
    object Database {
        const val LOAD_USER_ERROR = "Error loading cached user"
        const val CACHE_USER_ERROR = "Error caching user data"
        const val SIGN_OUT_ERROR = "Error during sign out"
    }

    // Format helpers
    fun format(errorMessage: String, vararg args: Any?): String {
        return if (args.isNotEmpty()) {
            String.format(errorMessage, *args)
        } else {
            errorMessage
        }
    }
}