// data/repository/ImageUploadRepository.kt
package com.example.skoolswap.data.repository

import android.content.Context
import android.net.Uri
import com.example.skoolswap.data.remote.api.ImageApiService
import com.example.skoolswap.data.remote.models.response.item.AttachImagesByUrlRequest
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.utils.ImageMultipartHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUploadRepository @Inject constructor(
    private val imageApiService: ImageApiService,
    private val authRepository: AuthRepositoryInterface  // ✅ Add this
) {

    suspend fun uploadImages(context: Context, imageUris: List<Uri>): List<String> {
        if (imageUris.isEmpty()) return emptyList()

        val imageParts = ImageMultipartHelper.createImageParts(context, imageUris)
        if (imageParts.isEmpty()) return emptyList()

        val urls = mutableListOf<String>()
        for (imagePart in imageParts) {
            try {
                val response = imageApiService.uploadImage(imagePart)
                if (response.isSuccessful) {
                    response.body()?.url?.let { urls.add(it) }
                }
            } catch (e: Exception) {
                // Log but continue
            }
        }
        return urls
    }

    suspend fun uploadAndAttachImages(
        context: Context,
        itemId: String,
        imageUris: List<Uri>
    ): Result<List<String>> {
        return try {
            // 1. Upload to R2
            val uploadedUrls = uploadImages(context, imageUris)
            if (uploadedUrls.isEmpty()) {
                return Result.success(emptyList())
            }

            // 2. Get auth token
            val token = authRepository.getAuthToken().value
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            // 3. Attach to item
            val response = imageApiService.attachImagesByUrl(
                "Bearer $token",
                itemId,
                AttachImagesByUrlRequest(uploadedUrls)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(uploadedUrls)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to attach images"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}