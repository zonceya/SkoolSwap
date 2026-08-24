package za.co.skoolswap.data.remote.api

import za.co.skoolswap.data.remote.models.response.item.AddImagesResponse
import za.co.skoolswap.data.remote.models.response.item.AttachImagesByUrlRequest
import za.co.skoolswap.data.remote.models.response.item.ImageUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ImageApiService {

    // ✅ CORRECTED - Use {item_id} with @Path
    @Multipart
    @POST("api/v1/items/{item_id}/images")
    suspend fun addItemImages(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String,  // ← @Path matches {item_id}
        @Part images: List<MultipartBody.Part>  // ← Supports multiple images
    ): Response<AddImagesResponse>

    // ✅ Keep this - attach by URL (alternative flow)
    @POST("api/v1/items/{item_id}/attach_images_by_url")
    suspend fun attachImagesByUrl(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String,
        @Body request: AttachImagesByUrlRequest
    ): Response<AddImagesResponse>
}