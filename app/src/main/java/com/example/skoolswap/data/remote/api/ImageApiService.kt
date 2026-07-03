package com.example.skoolswap.data.remote.api

import com.example.skoolswap.data.remote.models.response.item.AddImagesResponse
import com.example.skoolswap.data.remote.models.response.item.AttachImagesByUrlRequest
import com.example.skoolswap.data.remote.models.response.item.ImageUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// ImageApiService.kt - Add upload endpoint

interface ImageApiService {

    // ✅ ADD THIS - Upload images to R2
    @Multipart
    @POST("api/v1/uploads")
    suspend fun uploadImage(
        @Header("Authorization") authHeader: String,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>

    // ✅ Keep this
    @POST("api/v1/items/{item_id}/attach_images_by_url")
    suspend fun attachImagesByUrl(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String,
        @Body request: AttachImagesByUrlRequest
    ): Response<AddImagesResponse>
}