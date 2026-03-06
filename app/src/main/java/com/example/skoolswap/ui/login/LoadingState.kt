package com.example.skoolswap.ui.login

/**
 * Represents the loading state of the login process
 */
enum class LoadingState {
    /**
     * No operation in progress - initial state
     */
    IDLE,

    /**
     * Login operation is in progress
     */
    LOADING,

    /**
     * Login completed successfully
     */
    SUCCESS,

    /**
     * Login failed with error
     */
    ERROR
}