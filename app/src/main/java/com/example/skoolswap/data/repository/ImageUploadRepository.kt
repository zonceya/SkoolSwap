package com.example.skoolswap.data.repository

import android.content.Context
import android.net.Uri
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.data.remote.api.ImageApiService
import com.example.skoolswap.data.remote.models.response.item.AttachImagesByUrlRequest
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.utils.ImageMultipartHelper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUploadRepository @Inject constructor(
    private val imageApiService: ImageApiService,
    private val authRepository: AuthRepositoryInterface
) {

    suspend fun uploadImages(context: Context, imageUris: List<Uri>): List<String> {
        if (imageUris.isEmpty()) return emptyList()

        val imageParts = ImageMultipartHelper.createImageParts(context, imageUris)
        if (imageParts.isEmpty()) return emptyList()

        val urls = mutableListOf<String>()
        for (imagePart in imageParts) {
            try {
                val token = authRepository.getAuthToken().value
                if (token == null) {
                    Timber.tag(LogTags.REPOSITORY).e("No auth token")
                    continue
                }

                // TODO: Add your R2 upload endpoint here
                // val response = imageApiService.uploadImage("Bearer $token", imagePart)
                // if (response.isSuccessful) {
                //     response.body()?.url?.let { urls.add(it) }
                // }
            } catch (e: Exception) {
                Timber.tag(LogTags.REPOSITORY).e(e, "Upload failed")
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
                Timber.tag(LogTags.REPOSITORY).d("No images uploaded")
                return Result.success(emptyList())
            }

            // 2. Get auth token
            val token = authRepository.getAuthToken().value
            if (token == null) {
                Timber.tag(LogTags.REPOSITORY).e("No auth token")
                return Result.failure(Exception("Not authenticated"))
            }

            // 3. Attach to item
            val response = imageApiService.attachImagesByUrl(
                authHeader = "Bearer $token",
                itemId = itemId,
                request = AttachImagesByUrlRequest(uploadedUrls)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Timber.tag(LogTags.REPOSITORY).d("✅ Uploaded and attached ${uploadedUrls.size} images")
                Result.success(uploadedUrls)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to attach images"
                Timber.tag(LogTags.REPOSITORY).e("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Failed to upload and attach images")
            Result.failure(e)
        }
    }
}