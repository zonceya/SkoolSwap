package za.co.skoolswap.data.repository

import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.local.database.dao.ProvinceDao
import za.co.skoolswap.data.local.database.dao.SchoolDao
import za.co.skoolswap.data.local.database.entities.ProvinceEntity
import za.co.skoolswap.data.local.database.entities.SchoolEntity
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.data.remote.api.ProvinceApiService
import za.co.skoolswap.data.remote.api.SchoolApiService
import za.co.skoolswap.domain.model.Province
import za.co.skoolswap.domain.model.School
import za.co.skoolswap.domain.repository.SchoolRepositoryInterface
import za.co.skoolswap.utils.Result as AppResult  // ✅ Alias to avoid conflict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepository @Inject constructor(
    private val schoolApiService: SchoolApiService,
    private val provinceApiService: ProvinceApiService,
    private val provinceDao: ProvinceDao,
    private val schoolDao: SchoolDao,
    private val appPreferences: AppPreferences
) : SchoolRepositoryInterface {

    override suspend fun getProvinces(): AppResult<List<Province>> {
        return try {
            val cacheCount = provinceDao.getCount()
            Timber.tag(LogTags.REPOSITORY).d("🔍 Province cache count: $cacheCount")

            val cachedProvinces = provinceDao.getAllSync()

            if (cachedProvinces.isNotEmpty()) {
                Timber.tag(LogTags.REPOSITORY).d("✅ Using CACHED provinces: ${cachedProvinces.size}")
                refreshProvincesInBackground()
                return AppResult.Success(cachedProvinces.map {
                    Province(id = it.id, name = it.name)
                })
            }

            Timber.tag(LogTags.REPOSITORY).d("📡 Fetching provinces from API (cache empty)")
            val token = appPreferences.authToken.firstOrNull()
                ?: return AppResult.Error(Exception("No auth token"))

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

                Timber.tag(LogTags.REPOSITORY).d("📥 Received ${provinces.size} provinces from API")

                val entities = provinces.map {
                    ProvinceEntity(id = it.id, name = it.name)
                }
                provinceDao.insertAll(entities)

                AppResult.Success(provinces)
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Failed to load provinces: ${response.code()}")
                AppResult.Error(Exception("Failed to load provinces: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Error loading provinces")
            AppResult.Error(e)
        }
    }

    override fun refreshProvincesInBackground() {
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

    override suspend fun searchSchools(provinceId: Int, query: String): AppResult<List<School>> {
        return try {
            val token = appPreferences.authToken.firstOrNull()
                ?: return AppResult.Error(Exception("No auth token"))
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
                AppResult.Success(schools)
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ Search failed: ${response.code()}")
                AppResult.Error(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Error searching schools")
            AppResult.Error(e)
        }
    }

    // SchoolRepository.kt - searchSchoolsByTown
    override suspend fun searchSchoolsByTown(
        provinceId: Int,
        townName: String,
        schoolQuery: String?
    ): AppResult<List<School>> {
        return try {
            val token = appPreferences.authToken.firstOrNull()
                ?: return AppResult.Error(Exception("No auth token"))

            // ✅ Log the request for debugging
            Timber.tag(LogTags.REPOSITORY).d("🔍 API Request: provinceId=$provinceId, townName=$townName, schoolQuery=$schoolQuery")

            val response = schoolApiService.searchSchoolsByTown(
                authHeader = "Bearer $token",
                provinceId = provinceId,
                townName = townName,
                schoolQuery = schoolQuery
            )

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

                // ✅ Log the results
                Timber.tag(LogTags.REPOSITORY).d("🔍 API Response: ${schools.size} schools found")
                schools.forEach { school ->
                    Timber.tag(LogTags.REPOSITORY).d("  - ${school.name} (ID: ${school.id})")
                }

                // Cache results
                val entities = schools.map { school ->
                    SchoolEntity(
                        id = school.id,
                        name = school.name,
                        provinceId = school.provinceId,
                        locationId = school.locationId,
                        schoolType = school.schoolType,
                        logoUrl = school.logoUrl
                    )
                }
                if (entities.isNotEmpty()) {
                    schoolDao.insertAll(entities)
                }

                AppResult.Success(schools)
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ API Error: ${response.code()} - ${response.message()}")
                AppResult.Error(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Error searching schools by town")
            AppResult.Error(e)
        }
    }

    override suspend fun getSchoolById(id: Int): School? {
        return try {
            schoolDao.getById(id)?.let { entity ->
                School(
                    id = entity.id,
                    name = entity.name,
                    provinceId = entity.provinceId,
                    provinceName = null,
                    locationId = entity.locationId,
                    schoolType = entity.schoolType,
                    logoUrl = entity.logoUrl
                )
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "Error getting school by ID")
            null
        }
    }
}