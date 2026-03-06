// data/repository/SchoolRepository.kt
package com.example.skoolswap.data.repository

import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.api.SchoolApiService
import com.example.skoolswap.data.remote.api.ProvinceApiService
import com.example.skoolswap.domain.model.Province
import com.example.skoolswap.domain.model.School
import com.example.skoolswap.utils.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SchoolRepository @Inject constructor(
    private val schoolApiService: SchoolApiService,
    private val provinceApiService: ProvinceApiService,
    private val appPreferences: AppPreferences
) {

    suspend fun getProvinces(): Result<List<Province>> {
        return try {
            val token = appPreferences.authToken.first() ?: return Result.Error(Exception("No auth token"))
            val response = provinceApiService.getProvinces("Bearer $token")

            if (response.isSuccessful) {
                val provinces = response.body() ?: emptyList()
                Result.Success(provinces)
            } else {
                Result.Error(Exception("Failed to load provinces: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun searchSchools(provinceId: Int, query: String): Result<List<School>> {
        return try {
            val token = appPreferences.authToken.first() ?: return Result.Error(Exception("No auth token"))
            val response = schoolApiService.searchSchools("Bearer $token", provinceId, query)

            if (response.isSuccessful) {
                val body = response.body()
                val schools = body?.schools?.map { schoolResponse ->
                    School(
                        id = schoolResponse.id,
                        name = schoolResponse.name,
                        provinceId = schoolResponse.province_id,
                        provinceName = schoolResponse.province?.name,
                        locationId = schoolResponse.location_id,
                        schoolType = schoolResponse.school_type
                    )
                } ?: emptyList()
                Result.Success(schools)
            } else {
                Result.Error(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}