package com.example.skoolswap.data.remote.api

import com.example.skoolswap.domain.model.Province
import retrofit2.http.GET
import retrofit2.http.Header

interface ProvinceApiService {
    @GET("api/v1/provinces")
    suspend fun getProvinces(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<Province>>
}