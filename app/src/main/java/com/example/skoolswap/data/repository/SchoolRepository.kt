package com.example.skoolswap.data.repository

import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.data.local.database.dao.ProvinceDao
import com.example.skoolswap.data.local.database.entities.ProvinceEntity
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.api.SchoolApiService
import com.example.skoolswap.data.remote.api.ProvinceApiService
import com.example.skoolswap.domain.model.Province
import com.example.skoolswap.domain.model.School
import com.example.skoolswap.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SchoolRepository @Inject constructor(
    private val schoolApiService: SchoolApiService,
    private val provinceApiService: ProvinceApiService,
    private val provinceDao: ProvinceDao,
    private val appPreferences: AppPreferences
) {

    suspend fun getProvinces(): Result<List<Province>> {
        return try {
            val cacheCount = provinceDao.getCount()
            Timber.tag(LogTags.REPOSITORY).d("🔍 Province cache count: $cacheCount")

            val cachedProvinces = provinceDao.getAllSync()

            if (cachedProvinces.isNotEmpty()) {
                Timber.tag(LogTags.REPOSITORY).d("✅ Using CACHED provinces: ${cachedProvinces.size}")
                cachedProvinces.forEach {
                    Timber.tag(LogTags.REPOSITORY).d("  - Cached: ${it.name} (ID: ${it.id})")
                }

                refreshProvincesInBackground()

                return Result.Success(cachedProvinces.map {
                    Province(id = it.id, name = it.name)
                })
            }

            Timber.tag(LogTags.REPOSITORY).d("📡 Fetching provinces from API (cache empty)")
            val token = appPreferences.authToken.firstOrNull()
                ?: return Result.Error(Exception("No auth token"))

            val response = provinceApiService.getProvinces("Bearer $token")

            if (response.isSuccessful) {
                val provinces = response.body() ?: emptyList()
                Timber.tag(LogTags.REPOSITORY).d("📥 Received ${provinces.size} provinces from API")

                val entities = provinces.map {
                    ProvinceEntity(id = it.id, name = it.name)
                }
                provinceDao.insertAll(entities)

                val afterCount = provinceDao.getCount()
                Timber.tag(LogTags.REPOSITORY).d("✅ Cached $afterCount provinces")

                Result.Success(provinces)
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Failed to load provinces: ${response.code()}")
                Result.Error(Exception("Failed to load provinces: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Error loading provinces")
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
                    Timber.tag(LogTags.REPOSITORY).d("🔄 Provinces refreshed in background")
                }
            } catch (e: Exception) {
                Timber.tag(LogTags.REPOSITORY).e(e, "Background refresh failed")
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

                Timber.tag(LogTags.REPOSITORY).d("🔍 Found ${schools.size} schools for query: '$query'")
                Result.Success(schools)
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Search failed: ${response.code()}")
                Result.Error(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Error searching schools")
            Result.Error(e)
        }
    }
}