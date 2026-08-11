package za.co.skoolswap.data.repository

import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.local.database.dao.ProvinceDao
import za.co.skoolswap.data.local.database.entities.ProvinceEntity
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.data.remote.api.SchoolApiService
import za.co.skoolswap.data.remote.api.ProvinceApiService
import za.co.skoolswap.domain.model.Province
import za.co.skoolswap.domain.model.School
import za.co.skoolswap.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import za.co.skoolswap.data.local.database.dao.SchoolDao
import za.co.skoolswap.data.local.database.entities.SchoolEntity
import javax.inject.Inject

class SchoolRepository @Inject constructor(
    private val schoolApiService: SchoolApiService,
    private val provinceApiService: ProvinceApiService,
    private val provinceDao: ProvinceDao,
    private val schoolDao: SchoolDao,
    private val appPreferences: AppPreferences
) {

    // data/repository/SchoolRepository.kt - Updated getProvinces()
    suspend fun getProvinces(): Result<List<Province>> {
        return try {
            val cacheCount = provinceDao.getCount()
            Timber.tag(LogTags.REPOSITORY).d("🔍 Province cache count: $cacheCount")

            val cachedProvinces = provinceDao.getAllSync()

            if (cachedProvinces.isNotEmpty()) {
                Timber.tag(LogTags.REPOSITORY).d("✅ Using CACHED provinces: ${cachedProvinces.size}")
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
                val provinceListResponse = response.body()
                val provinceResponses = provinceListResponse?.provinces ?: emptyList()

                // Convert ProvinceResponse to Province
                val provinces = provinceResponses.map { provinceResponse ->
                    Province(
                        id = provinceResponse.id,
                        name = provinceResponse.name
                    )
                }

                Timber.tag(LogTags.REPOSITORY).d("📥 Received ${provinces.size} provinces from API")

                // Cache the provinces
                val entities = provinces.map {
                    ProvinceEntity(id = it.id, name = it.name)
                }
                provinceDao.insertAll(entities)

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

    // Also update refreshProvincesInBackground()
    private fun refreshProvincesInBackground() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = appPreferences.authToken.firstOrNull() ?: return@launch
                val response = provinceApiService.getProvinces("Bearer $token")

                if (response.isSuccessful) {
                    val provinceListResponse = response.body()
                    val provinceResponses = provinceListResponse?.provinces ?: emptyList()

                    val provinces = provinceResponses.map { provinceResponse ->
                        Province(
                            id = provinceResponse.id,
                            name = provinceResponse.name
                        )
                    }

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
                        schoolType = schoolResponse.school_type,
                        logoUrl = schoolResponse.logo_url
                    )
                } ?: emptyList()
                val entities = schools.map {
                    SchoolEntity(
                        id = it.id,
                        name = it.name,
                        provinceId = it.provinceId,
                        locationId = it.locationId,
                        schoolType = it.schoolType,
                        logoUrl = it.logoUrl
                    )
                }
                schoolDao.insertAll(entities)
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