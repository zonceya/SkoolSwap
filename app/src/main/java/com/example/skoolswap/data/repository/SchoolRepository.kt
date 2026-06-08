// data/repository/SchoolRepository.kt
package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.local.database.dao.ProvinceDao
import com.example.skoolswap.data.local.database.entities.ProvinceEntity
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.api.SchoolApiService
import com.example.skoolswap.data.remote.api.ProvinceApiService
import com.example.skoolswap.domain.model.Province
import com.example.skoolswap.domain.model.School
import com.example.skoolswap.utils.Result
import com.example.skoolswap.data.mapper.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

class SchoolRepository @Inject constructor(
    private val schoolApiService: SchoolApiService,
    private val provinceApiService: ProvinceApiService,
    private val provinceDao: ProvinceDao,
    private val appPreferences: AppPreferences
) {

    suspend fun getProvinces(): Result<List<Province>> {
        return try {
            // STEP 1: Check cache with count
            val cacheCount = provinceDao.getCount()
            Log.d("SchoolRepo", "🔍 Province cache count: $cacheCount")

            val cachedProvinces = provinceDao.getAllSync()

            if (cachedProvinces.isNotEmpty()) {
                Log.d("SchoolRepo", "✅ Using CACHED provinces: ${cachedProvinces.size}")
                cachedProvinces.forEach {
                    Log.d("SchoolRepo", "  - Cached: ${it.name} (ID: ${it.id})")
                }

                // Refresh in background
                refreshProvincesInBackground()

                return Result.Success(cachedProvinces.map {
                    Province(id = it.id, name = it.name)
                })
            }

            // STEP 2: No cache - fetch from API
            Log.d("SchoolRepo", "📡 Fetching provinces from API (cache empty)")
            val token = appPreferences.authToken.firstOrNull()
                ?: return Result.Error(Exception("No auth token"))

            val response = provinceApiService.getProvinces("Bearer $token")

            if (response.isSuccessful) {
                val provinces = response.body() ?: emptyList()
                Log.d("SchoolRepo", "📥 Received ${provinces.size} provinces from API")

                // Cache them
                val entities = provinces.map {
                    ProvinceEntity(id = it.id, name = it.name)
                }
                provinceDao.insertAll(entities)

                // Verify cache
                val afterCount = provinceDao.getCount()
                Log.d("SchoolRepo", "✅ Cached $afterCount provinces")

                Result.Success(provinces)
            } else {
                Result.Error(Exception("Failed to load provinces: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("SchoolRepo", "Error loading provinces", e)
            Result.Error(e)
        }
    }
    private fun refreshProvincesInBackground() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = appPreferences.authToken.firstOrNull() ?: return@launch
                val response = provinceApiService.getProvinces("Bearer $token")

                if (response.isSuccessful) {
                    val provinces = response.body() ?: return@launch
                    val entities = provinces.map {
                        ProvinceEntity(id = it.id, name = it.name)
                    }
                    provinceDao.insertAll(entities)
                    Log.d("SchoolRepo", "🔄 Provinces refreshed in background")
                }
            } catch (e: Exception) {
                Log.e("SchoolRepo", "Background refresh failed", e)
            }
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