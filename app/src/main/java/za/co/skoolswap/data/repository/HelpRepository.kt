// com/example/skoolswap/data/repository/HelpRepository.kt
package za.co.skoolswap.data.repository

import za.co.skoolswap.common.constants.ErrorConstants
import za.co.skoolswap.common.constants.ErrorConstantsHelper
import za.co.skoolswap.data.remote.api.HelpApiService
import za.co.skoolswap.data.remote.models.request.HelpRequest
import za.co.skoolswap.data.remote.models.response.help.HelpResponse
import za.co.skoolswap.data.remote.models.response.help.HelpStatusResponse
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.HelpRepositoryInterface
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HelpRepository @Inject constructor(
    private val helpApiService: HelpApiService,
    private val authRepository: AuthRepositoryInterface
) : HelpRepositoryInterface {

    companion object {
        private const val TAG = "HelpRepository"
    }

    override suspend fun sendSupportRequest(
        subject: String,
        description: String
    ): Result<HelpResponse> {
        return try {
            val token = authRepository.getAuthToken().value
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception(ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED))
            }

            if (subject.isBlank()) {
                return Result.failure(Exception(ErrorConstants.Messages.UserFriendly.INVALID_INPUT))
            }

            if (description.isBlank()) {
                return Result.failure(Exception(ErrorConstants.Messages.UserFriendly.INVALID_INPUT))
            }

            if (description.length > 400) {
                return Result.failure(Exception("Description cannot exceed 400 characters"))
            }

            val request = HelpRequest(
                subject = subject.trim(),
                description = description.trim()
            )

            val response = helpApiService.sendSupportRequest(
                authToken = "Bearer $token",
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception(getErrorMessageFromCode(response.code())))
            }

        } catch (e: HttpException) {
            Result.failure(Exception(getErrorMessageFromCode(e.code())))
        } catch (e: IOException) {
            Result.failure(Exception(ErrorConstants.Messages.UserFriendly.NO_INTERNET))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorConstantsHelper.getErrorMessage(e)))
        } as Result<HelpResponse>
    }

    override suspend fun getHelpStatus(): Result<HelpStatusResponse> {
        return try {
            val response = helpApiService.getHelpStatus()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception("Failed to get status: ${response.code()}"))
            }

        } catch (e: Exception) {
            Result.failure(Exception(ErrorConstantsHelper.getErrorMessage(e)))
        } as Result<HelpStatusResponse>
    }

    private fun getErrorMessageFromCode(code: Int): String {
        return when (code) {
            ErrorConstants.HttpStatus.BAD_REQUEST -> ErrorConstants.Messages.UserFriendly.INVALID_INPUT
            ErrorConstants.HttpStatus.UNAUTHORIZED -> ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED
            ErrorConstants.HttpStatus.FORBIDDEN -> ErrorConstants.Messages.UserFriendly.ACCESS_DENIED
            ErrorConstants.HttpStatus.NOT_FOUND -> ErrorConstants.Messages.UserFriendly.NOT_FOUND
            ErrorConstants.HttpStatus.TOO_MANY_REQUESTS -> ErrorConstants.Messages.UserFriendly.SERVER_BUSY
            ErrorConstants.HttpStatus.INTERNAL_SERVER,
            ErrorConstants.HttpStatus.BAD_GATEWAY,
            ErrorConstants.HttpStatus.SERVICE_UNAVAILABLE -> ErrorConstants.Messages.UserFriendly.SERVER_DOWN
            else -> "${ErrorConstants.Messages.UserFriendly.UNKNOWN} ($code)"
        }
    }
}