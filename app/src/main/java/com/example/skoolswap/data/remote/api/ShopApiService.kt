package com.example.skoolswap.data.remote.api

import com.example.skoolswap.data.remote.models.request.UpdateShopRequest
import com.example.skoolswap.data.remote.models.response.shop.PublicShopResponse
import com.example.skoolswap.data.remote.models.response.shop.ShopResponse
import com.example.skoolswap.data.remote.models.response.shop.UpdateShopResponse

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ShopApiService {

    @GET("api/v1/shop")
    suspend fun getMyShop(
        @Header("Authorization") authToken: String
    ): Response<ShopResponse>

    @PATCH("api/v1/shop")
    suspend fun updateShop(
        @Header("Authorization") authToken: String,
        @Body request: UpdateShopRequest
    ): Response<UpdateShopResponse>

    @GET("api/v1/shops/{id}")
    suspend fun getPublicShop(
        @Path("id") shopId: Long
    ): Response<PublicShopResponse>
}