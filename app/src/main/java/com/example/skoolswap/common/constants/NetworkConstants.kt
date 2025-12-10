package com.example.skoolswap.common.constants

object NetworkConstants {
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    // API Endpoints
    object Endpoints {
        const val SIGN_IN = "api/v1/users/sign_in"
        const val SIGN_UP = "api/v1/users/sign_up"
        const val USER_PROFILE = "api/v1/users/{id}/profile"
        const val SHOPS = "api/v1/shops"
        const val ORDERS = "api/v1/orders"
    }

    // HTTP Status Codes
    object StatusCodes {
        const val SUCCESS = 200
        const val CREATED = 201
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val INTERNAL_ERROR = 500
    }
}