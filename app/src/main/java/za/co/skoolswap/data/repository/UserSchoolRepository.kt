package za.co.skoolswap.data.repository

import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.local.database.dao.ProvinceDao
import za.co.skoolswap.data.local.database.dao.SchoolDao
import za.co.skoolswap.data.local.database.dao.UserDao
import za.co.skoolswap.data.local.database.dao.UserSchoolDao
import za.co.skoolswap.data.local.database.entities.SchoolEntity
import za.co.skoolswap.data.local.database.entities.UserSchoolEntity
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.data.remote.api.UserSchoolApiService
import za.co.skoolswap.domain.model.SchoolMapping
import za.co.skoolswap.domain.repository.UserSchoolRepositoryInterface
import za.co.skoolswap.utils.Result
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class UserSchoolRepository @Inject constructor(
    private val userSchoolApiService: UserSchoolApiService,
    private val userSchoolDao: UserSchoolDao,
    private val schoolDao: SchoolDao,
    private val provinceDao: ProvinceDao,
    private val userDao: UserDao,
    private val appPreferences: AppPreferences
) : UserSchoolRepositoryInterface {

    override suspend fun assignSchool(schoolId: Int): Result<Unit> {
        return try {
            val token = appPreferences.authToken.firstOrNull()
                ?: return Result.Error(Exception("No auth token"))

            Timber.tag(LogTags.REPOSITORY).d("📚 Assigning school: $schoolId")
            val response = userSchoolApiService.assignSchool(
                "Bearer $token",
                mapOf("school_id" to schoolId)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.school != null) {
                    val school = body.school
                    val userId = appPreferences.getUserId()
                        ?: return Result.Error(Exception("No user ID"))

                    val userSchoolEntity = UserSchoolEntity(
                        id = school.mapping_id,
                        userId = userId,
                        schoolId = school.id,
                        schoolName = school.name,
                        mappedAt = school.mapped_at,
                        updatedAt = school.updated_at
                    )
                    userSchoolDao.insert(userSchoolEntity)

                    val schoolEntity = SchoolEntity(
                        id = school.id,
                        name = school.name,
                        provinceId = school.province_id,
                        schoolType = school.school_type
                    )
                    schoolDao.insertAll(listOf(schoolEntity))

                    appPreferences.setSchoolMapped(true)
                    appPreferences.setSchoolInfo(school.id, school.name)
                    appPreferences.setSchoolMappingId(school.mapping_id)

                    syncSchoolToUserEntity(userId, school.id, school.name)

                    Timber.tag(LogTags.REPOSITORY).d("✅ School assigned successfully: ${school.name}")
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body?.message ?: "Failed to assign school"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Error assigning school")
            Result.Error(e)
        }
    }

    override suspend fun getCurrentSchoolMapping(): Result<SchoolMapping?> {
        return try {
            Timber.tag(LogTags.REPOSITORY).d("🔍 Getting current school mapping")

            val userId = appPreferences.getUserId()

            if (userId != null) {
                val cachedMapping = userSchoolDao.getCurrentForUserSync(userId)
                if (cachedMapping != null) {
                    if (cachedMapping.id.startsWith("temp_")) {
                        Timber.tag(LogTags.REPOSITORY).d("⚠️ Using TEMP cache, refreshing in background")

                        val mapping = SchoolMapping(
                            mappingId = cachedMapping.id,
                            schoolId = cachedMapping.schoolId,
                            schoolName = cachedMapping.schoolName,
                            provinceId = null,
                            locationId = null,
                            schoolType = null,
                            mappedAt = cachedMapping.mappedAt,
                            updatedAt = cachedMapping.updatedAt
                        )

                        refreshInBackground(userId)
                        return Result.Success(mapping)
                    } else {
                        Timber.tag(LogTags.REPOSITORY).d("✅ Using REAL cache: ${cachedMapping.schoolName}")

                        val mapping = SchoolMapping(
                            mappingId = cachedMapping.id,
                            schoolId = cachedMapping.schoolId,
                            schoolName = cachedMapping.schoolName,
                            provinceId = null,
                            locationId = null,
                            schoolType = null,
                            mappedAt = cachedMapping.mappedAt,
                            updatedAt = cachedMapping.updatedAt
                        )

                        refreshInBackground(userId)
                        return Result.Success(mapping)
                    }
                } else {
                    Timber.tag(LogTags.REPOSITORY).d("⚠️ No cache found for user $userId")
                }
            }

            Timber.tag(LogTags.REPOSITORY).d("📡 Fetching from API")
            return fetchFromApi()

        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Error getting school mapping")
            Result.Error(e)
        }
    }

    private suspend fun fetchFromApi(): Result<SchoolMapping?> {
        val token = appPreferences.authToken.firstOrNull()
            ?: return Result.Error(Exception("No auth token"))

        val response = userSchoolApiService.getCurrentSchool("Bearer $token")

        if (response.isSuccessful) {
            val body = response.body()
            if (body?.school_mapped == true && body.school != null) {
                val school = body.school
                val userId = appPreferences.getUserId()

                if (userId != null) {
                    val entity = UserSchoolEntity(
                        id = school.mapping_id,
                        userId = userId,
                        schoolId = school.id,
                        schoolName = school.name,
                        mappedAt = school.mapped_at,
                        updatedAt = school.updated_at
                    )
                    userSchoolDao.insert(entity)

                    val schoolEntity = SchoolEntity(
                        id = school.id,
                        name = school.name,
                        provinceId = school.province_id,
                        schoolType = school.school_type
                    )
                    schoolDao.insertAll(listOf(schoolEntity))
                }

                return Result.Success(
                    SchoolMapping(
                        mappingId = school.mapping_id,
                        schoolId = school.id,
                        schoolName = school.name,
                        provinceId = school.province_id,
                        locationId = school.location_id,
                        schoolType = school.school_type,
                        mappedAt = school.mapped_at,
                        updatedAt = school.updated_at
                    )
                )
            } else {
                appPreferences.setSchoolMapped(false)
                return Result.Success(null)
            }
        }

        return Result.Error(Exception("API error: ${response.code()}"))
    }

    private fun refreshInBackground(userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.tag(LogTags.REPOSITORY).d("🔄 Background refresh started")
                val token = appPreferences.authToken.firstOrNull() ?: return@launch
                val response = userSchoolApiService.getCurrentSchool("Bearer $token")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.school_mapped == true && body.school != null) {
                        val school = body.school

                        val entity = UserSchoolEntity(
                            id = school.mapping_id,
                            userId = userId,
                            schoolId = school.id,
                            schoolName = school.name,
                            mappedAt = school.mapped_at,
                            updatedAt = school.updated_at
                        )
                        userSchoolDao.insert(entity)

                        Timber.tag(LogTags.REPOSITORY).d("✅ Background refresh complete: ${school.name}")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(LogTags.REPOSITORY).e(e, "⚠️ Background refresh failed")
            }
        }
    }

    override suspend fun updateSchoolMapping(mappingId: String, schoolId: Int): Result<SchoolMapping> {
        return try {
            Timber.tag(LogTags.REPOSITORY).d("🔄 Updating school mapping: $mappingId to school $schoolId")

            val token = appPreferences.authToken.firstOrNull()
            if (token.isNullOrEmpty()) {
                return Result.Error(Exception("No auth token"))
            }

            val response = userSchoolApiService.updateSchool(
                "Bearer $token",
                mappingId,
                mapOf("school_id" to schoolId)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.school != null) {
                    val school = body.school
                    Timber.tag(LogTags.REPOSITORY).d("✅ School updated to: ${school.name}")

                    val mapping = SchoolMapping(
                        mappingId = school.mapping_id,
                        schoolId = school.id,
                        schoolName = school.name,
                        provinceId = school.province_id,
                        locationId = school.location_id,
                        schoolType = school.school_type,
                        mappedAt = school.mapped_at,
                        updatedAt = school.updated_at
                    )

                    appPreferences.setSchoolMapped(true)
                    appPreferences.setSchoolInfo(school.id, school.name)
                    appPreferences.setSchoolMappingId(school.mapping_id)

                    val userId = appPreferences.getUserId()
                    if (userId != null) {
                        syncSchoolToUserEntity(userId, school.id, school.name)
                    }

                    Result.Success(mapping)
                } else {
                    Timber.tag(LogTags.REPOSITORY).e("❌ Update failed: ${body?.message}")
                    Result.Error(Exception(body?.message ?: "Failed to update school"))
                }
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ HTTP error: ${response.code()}")
                Result.Error(Exception("Update failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Exception updating school")
            Result.Error(e)
        }
    }

    override suspend fun removeSchoolMapping(mappingId: String): Result<Boolean> {
        return try {
            Timber.tag(LogTags.REPOSITORY).d("🗑️ Removing school mapping: $mappingId")

            val token = appPreferences.authToken.firstOrNull()
            if (token.isNullOrEmpty()) {
                return Result.Error(Exception("No auth token"))
            }

            val response = userSchoolApiService.removeSchool("Bearer $token", mappingId)

            if (response.isSuccessful) {
                Timber.tag(LogTags.REPOSITORY).d("✅ School mapping removed")

                appPreferences.setSchoolMapped(false)
                appPreferences.clearSchoolInfo()

                val userId = appPreferences.getUserId()
                if (userId != null) {
                    clearSchoolFromUserEntity(userId)
                }

                Result.Success(true)
            } else {
                Timber.tag(LogTags.REPOSITORY).e("❌ HTTP error: ${response.code()}")
                Result.Error(Exception("Remove mapping failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "❌ Exception removing school")
            Result.Error(e)
        }
    }

    // data/repository/UserSchoolRepositoryImpl.kt
    override suspend fun getNearbySchoolIds(schoolId: Int): List<Int> {
        return try {
            val school = schoolDao.getByIdSync(schoolId)
            if (school == null) return emptyList()

            // Priority 1: Same location
            school.locationId?.let { locationId ->
                val nearby = schoolDao.getByLocationId(locationId, schoolId)
                if (nearby.isNotEmpty()) {
                    return nearby.map { it.id }
                }
            }

            // Priority 2: Same province
            school.provinceId?.let { provinceId ->
                val nearby = schoolDao.getByProvinceId(provinceId, schoolId)
                if (nearby.isNotEmpty()) {
                    return nearby.map { it.id }
                }
            }

            emptyList()
        } catch (e: Exception) {
            Timber.tag("UserSchoolRepo").e("Failed to get nearby schools: ${e.message}")
            emptyList()
        }
    }

    private suspend fun clearSchoolFromUserEntity(userId: Int) {
        try {
            val currentUser = userDao.getCurrentUser()
            if (currentUser != null && currentUser.id == userId) {
                val updatedUser = currentUser.copy(
                    schoolMapped = false,
                    schoolId = null,
                    schoolName = null
                )
                userDao.insertUser(updatedUser)
                Timber.tag(LogTags.REPOSITORY).d("✅ Cleared school info from users table")
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "⚠️ Failed to clear school from UserEntity")
        }
    }

    private suspend fun syncSchoolToUserEntity(userId: Int, schoolId: Int, schoolName: String) {
        try {
            val currentUser = userDao.getCurrentUser()
            if (currentUser != null && currentUser.id == userId) {
                val updatedUser = currentUser.copy(
                    schoolMapped = true,
                    schoolId = schoolId,
                    schoolName = schoolName
                )
                userDao.insertUser(updatedUser)
                Timber.tag(LogTags.REPOSITORY).d("✅ Synced school mapping to users table: $schoolName")
            } else {
                Timber.tag(LogTags.REPOSITORY).w("⚠️ User not found in Room with ID: $userId")
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.REPOSITORY).e(e, "⚠️ Failed to sync school to UserEntity")
        }
    }
}