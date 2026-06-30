package com.example.skoolswap.data.remote.api

import com.example.skoolswap.data.remote.models.request.CreateItemRequest
import com.example.skoolswap.data.remote.models.request.UpdateItemRequest
import com.example.skoolswap.data.remote.models.response.item.*
import com.example.skoolswap.data.remote.models.response.shop.PublicShopItemsResponse
import com.example.skoolswap.data.remote.models.response.shop.ShopItemsResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ItemApiService {

    @POST("api/v1/items/createItems")
    suspend fun createItem(
        @Header("Authorization") authHeader: String,
        @Body request: CreateItemRequest
    ): Response<CreateItemResponse>
    @PUT("api/v1/items/{item_id}/updateItem")
    suspend fun updateItem(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String,
        @Body request: UpdateItemRequest
    ): Response<UpdateItemResponse>
    @Multipart
    @POST("api/v1/items/{item_id}/images")
    suspend fun addItemImages(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String,
        @Part images: List<MultipartBody.Part>
    ): Response<AddImagesResponse>

    @DELETE("api/v1/items/{item_id}/images/{image_id}")
    suspend fun removeItemImage(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String,
        @Path("image_id") imageId: Long
    ): Response<RemoveImageResponse>

    @GET("api/v1/items/{item_id}")
    suspend fun getItem(
        @Path("item_id") itemId: String
    ): Response<ItemDetailResponse>

    @GET("api/v1/items")
    suspend fun getItems(
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("main_category_id") mainCategoryId: Int? = null,
        @Query("sub_category_id") subCategoryId: Int? = null,
        @Query("province_id") provinceId: Int? = null,
        @Query("town_id") townId: Int? = null
    ): Response<List<CreateItemResponse>>

    @GET("api/v1/items/shop_items")  // ✅ CORRECT
    suspend fun getMyShopItems(
        @Header("Authorization") authToken: String
    ): Response<ShopItemsResponse>
    @GET("api/v1/shops/{shop_id}/items")
    suspend fun getShopItems(
        @Path("shop_id") shopId: Long
    ): Response<PublicShopItemsResponse>
    @GET("api/v1/items/{item_id}/viewShopItem")
    suspend fun getShopItemForEdit(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String
    ): Response<ViewShopItemResponse>
    // NEW: Delete item (soft delete)
    @DELETE("api/v1/items/{item_id}/deleteItem")
    suspend fun deleteItem(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String
    ): Response<DeleteItemResponse>

    // NEW: Mark item as sold
    @PATCH("api/v1/items/{item_id}/mark_as_sold")
    suspend fun markItemAsSold(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String
    ): Response<MarkAsSoldResponse>



}

