package za.co.skoolswap.common.constants

import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorConstantsHelper {

    /**
     * Maps HTTP status codes and exceptions to user-friendly error messages
     */
    fun getErrorMessage(throwable: Throwable?): String {
        return when (throwable) {
            null -> ErrorConstants.Messages.UserFriendly.UNKNOWN

            is HttpException -> {
                when (throwable.code()) {
                    ErrorConstants.HttpStatus.UNAUTHORIZED -> ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED
                    ErrorConstants.HttpStatus.FORBIDDEN -> ErrorConstants.Messages.UserFriendly.ACCESS_DENIED
                    ErrorConstants.HttpStatus.NOT_FOUND -> ErrorConstants.Messages.UserFriendly.NOT_FOUND
                    ErrorConstants.HttpStatus.TOO_MANY_REQUESTS -> ErrorConstants.Messages.UserFriendly.SERVER_BUSY
                    ErrorConstants.HttpStatus.INTERNAL_SERVER,
                    ErrorConstants.HttpStatus.BAD_GATEWAY,
                    ErrorConstants.HttpStatus.SERVICE_UNAVAILABLE -> ErrorConstants.Messages.UserFriendly.SERVER_DOWN
                    ErrorConstants.HttpStatus.AUTHENTICATION_TIMEOUT -> ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED
                    else -> "${ErrorConstants.Messages.UserFriendly.UNKNOWN} (${throwable.code()})"
                }
            }

            is SocketTimeoutException -> ErrorConstants.Messages.UserFriendly.TIMEOUT

            is UnknownHostException, is ConnectException -> ErrorConstants.Messages.UserFriendly.NO_INTERNET

            else -> throwable.message ?: ErrorConstants.Messages.UserFriendly.UNKNOWN
        }
    }

    /**
     * Creates an AppError from an exception
     */
    fun createAppError(throwable: Throwable?): ErrorConstants.AppError {
        return when (throwable) {
            null -> ErrorConstants.AppError.UnknownError()

            is HttpException -> {
                when (throwable.code()) {
                    ErrorConstants.HttpStatus.UNAUTHORIZED ->
                        ErrorConstants.AppError.AuthError(
                            technicalMessage = throwable.message()
                        )
                    ErrorConstants.HttpStatus.FORBIDDEN ->
                        ErrorConstants.AppError.AuthError(
                            userMessage = ErrorConstants.Messages.UserFriendly.ACCESS_DENIED,
                            technicalMessage = throwable.message()
                        )
                    ErrorConstants.HttpStatus.NOT_FOUND ->
                        ErrorConstants.AppError.ValidationError(
                            userMessage = ErrorConstants.Messages.UserFriendly.NOT_FOUND,
                            technicalMessage = throwable.message()
                        )
                    ErrorConstants.HttpStatus.BAD_REQUEST ->
                        ErrorConstants.AppError.ValidationError(
                            technicalMessage = throwable.message()
                        )
                    ErrorConstants.HttpStatus.INTERNAL_SERVER,
                    ErrorConstants.HttpStatus.BAD_GATEWAY,
                    ErrorConstants.HttpStatus.SERVICE_UNAVAILABLE ->
                        ErrorConstants.AppError.ServerError(
                            technicalMessage = throwable.message()
                        )
                    ErrorConstants.HttpStatus.AUTHENTICATION_TIMEOUT ->
                        ErrorConstants.AppError.AuthError(
                            userMessage = ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED,
                            technicalMessage = throwable.message(),
                            isRetryable = true,
                            action = ErrorConstants.ErrorAction.RETRY
                        )
                    else ->
                        ErrorConstants.AppError.UnknownError(
                            technicalMessage = "HTTP ${throwable.code()}: ${throwable.message()}"
                        )
                }
            }

            is SocketTimeoutException ->
                ErrorConstants.AppError.TimeoutError(
                    technicalMessage = throwable.message
                )

            is UnknownHostException, is ConnectException ->
                ErrorConstants.AppError.NetworkError(
                    technicalMessage = throwable.message
                )

            else ->
                ErrorConstants.AppError.UnknownError(
                    technicalMessage = throwable.message
                )
        }
    }

    /**
     * Checks if an error is retryable
     */
    fun isRetryable(error: ErrorConstants.AppError): Boolean {
        return error.isRetryable
    }

    /**
     * Gets the appropriate action for an error
     */
    fun getActionForError(error: ErrorConstants.AppError): ErrorConstants.ErrorAction {
        return error.action ?: when (error) {
            is ErrorConstants.AppError.NetworkError -> ErrorConstants.ErrorAction.RETRY
            is ErrorConstants.AppError.AuthError -> ErrorConstants.ErrorAction.SIGN_OUT
            is ErrorConstants.AppError.ValidationError -> ErrorConstants.ErrorAction.RETRY
            is ErrorConstants.AppError.ServerError -> ErrorConstants.ErrorAction.RETRY
            is ErrorConstants.AppError.TimeoutError -> ErrorConstants.ErrorAction.RETRY
            else -> ErrorConstants.ErrorAction.RETRY
        }
    }
}