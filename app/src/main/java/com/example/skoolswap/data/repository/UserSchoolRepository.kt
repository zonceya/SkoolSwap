// data/repository/UserSchoolRepositoryImpl.kt
package com.example.skoolswap.data.repository

import android.util.Log
import com.example.skoolswap.data.local.database.dao.ProvinceDao
import com.example.skoolswap.data.local.database.dao.SchoolDao
import com.example.skoolswap.data.local.database.dao.UserDao
import com.example.skoolswap.data.local.database.dao.UserSchoolDao
import com.example.skoolswap.data.local.database.entities.SchoolEntity
import com.example.skoolswap.data.local.database.entities.UserSchoolEntity
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.api.UserSchoolApiService
import com.example.skoolswap.domain.model.SchoolMapping
import com.example.skoolswap.domain.repository.UserSchoolRepositoryInterface
import com.example.skoolswap.utils.Result
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import com.example.skoolswap.data.mapper.toEntity  // For school.toEntity(userId)
import com.example.skoolswap.data.mapper.toSchoolEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class UserSchoolRepository @Inject constructor(
    private val userSchoolApiService: UserSchoolApiService,
    private val userSchoolDao: UserSchoolDao,
    private val schoolDao: SchoolDao,
    private val  provinceDao: ProvinceDao,
    private val userDao: UserDao,
    private val appPreferences: AppPreferences
) : UserSchoolRepositoryInterface {

    private companion object {
        private const val TAG = "UserSchoolRepo"
    }

    override suspend fun assignSchool(schoolId: Int): Result<Unit> {
        return try {
            val token = appPreferences.authToken.firstOrNull()
                ?: return Result.Error(Exception("No auth token"))

            Log.d(TAG, "📚 Assigning school: $schoolId")
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

                    // Save to user_schools table
                    val userSchoolEntity = UserSchoolEntity(
                        id = school.mapping_id,
                        userId = userId,
                        schoolId = school.id,
                        schoolName = school.name,
                        mappedAt = school.mapped_at,
                        updatedAt = school.updated_at
                    )
                    userSchoolDao.insert(userSchoolEntity)

                    // Save to schools table
                    val schoolEntity = SchoolEntity(
                        id = school.id,
                        name = school.name,
                        provinceId = school.province_id,
                        schoolType = school.school_type
                    )
                    schoolDao.insertAll(listOf(schoolEntity))

                    // Update preferences
                    appPreferences.setSchoolMapped(true)
                    appPreferences.setSchoolInfo(school.id, school.name)
                    appPreferences.setSchoolMappingId(school.mapping_id)

                    // ✅ CRITICAL: Sync to users table
                    syncSchoolToUserEntity(userId, school.id, school.name)

                    Log.d(TAG, "✅ School assigned successfully: ${school.name}")
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body?.message ?: "Failed to assign school"))
                }
            } else {
                Result.Error(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    // In UserSchoolRepository.kt - update getCurrentSchoolMapping
    override suspend fun getCurrentSchoolMapping(): Result<SchoolMapping?> {
        return try {
            Log.d(TAG, "🔍 Getting current school mapping")

            val userId = appPreferences.getUserId()

            // STEP 1: Try to get from cache
            if (userId != null) {
                val cachedMapping = userSchoolDao.getCurrentForUserSync(userId)
                if (cachedMapping != null) {
                    // If it's a temp ID, still use it but refresh in background
                    if (cachedMapping.id.startsWith("temp_")) {
                        Log.d(TAG, "⚠️ Using TEMP cache, refreshing in background")

                        // Return temp data immediately
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

                        // Refresh in background
                        refreshInBackground(userId)

                        return Result.Success(mapping)
                    } else {
                        Log.d(TAG, "✅ Using REAL cache: ${cachedMapping.schoolName}")

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

                        // Still refresh in background
                        refreshInBackground(userId)

                        return Result.Success(mapping)
                    }
                } else {
                    Log.d(TAG, "⚠️ No cache found for user $userId")
                }
            }

            // STEP 2: No cache - fetch from API
            Log.d(TAG, "📡 Fetching from API")
            return fetchFromApi()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}", e)
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
                    // Save to cache
                    val entity = UserSchoolEntity(
                        id = school.mapping_id,
                        userId = userId,
                        schoolId = school.id,
                        schoolName = school.name,
                        mappedAt = school.mapped_at,
                        updatedAt = school.updated_at
                    )
                    userSchoolDao.insert(entity)

                    // Also save to schools table
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
                Log.d(TAG, "🔄 Background refresh started")
                val token = appPreferences.authToken.firstOrNull() ?: return@launch
                val response = userSchoolApiService.getCurrentSchool("Bearer $token")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.school_mapped == true && body.school != null) {
                        val school = body.school

                        // Update cache with fresh data
                        val entity = UserSchoolEntity(
                            id = school.mapping_id,
                            userId = userId,
                            schoolId = school.id,
                            schoolName = school.name,
                            mappedAt = school.mapped_at,
                            updatedAt = school.updated_at
                        )
                        userSchoolDao.insert(entity)

                        Log.d(TAG, "✅ Background refresh complete: ${school.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Background refresh failed: ${e.message}")
            }
        }
    }
    override suspend fun updateSchoolMapping(mappingId: String, schoolId: Int): Result<SchoolMapping> {
        return try {
            Log.d(TAG, "🔄 Updating school mapping: $mappingId to school $schoolId")

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
                    Log.d(TAG, "✅ School updated to: ${school.name}")

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

                    // Update preferences
                    appPreferences.setSchoolMapped(true)
                    appPreferences.setSchoolInfo(school.id, school.name)
                    appPreferences.setSchoolMappingId(school.mapping_id)

                    // ✅ SYNC: Update users table
                    val userId = appPreferences.getUserId()
                    if (userId != null) {
                        syncSchoolToUserEntity(userId, school.id, school.name)
                    }

                    Result.Success(mapping)
                } else {
                    Log.e(TAG, "❌ Update failed: ${body?.message}")
                    Result.Error(Exception(body?.message ?: "Failed to update school"))
                }
            } else {
                Log.e(TAG, "❌ HTTP error: ${response.code()}")
                Result.Error(Exception("Update failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception: ${e.message}", e)
            Result.Error(e)
        }
    }
// UserSchoolRepository.kt - update removeSchoolMapping

    override suspend fun removeSchoolMapping(mappingId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "🗑️ Removing school mapping: $mappingId")

            val token = appPreferences.authToken.firstOrNull()
            if (token.isNullOrEmpty()) {
                return Result.Error(Exception("No auth token"))
            }

            val response = userSchoolApiService.removeSchool("Bearer $token", mappingId)

            if (response.isSuccessful) {
                Log.d(TAG, "✅ School mapping removed")

                // Clear preferences - use clearSchoolInfo() instead of clearSchoolInfo()
                appPreferences.setSchoolMapped(false)
                appPreferences.clearSchoolInfo()  // ✅ This method exists now

                // ✅ SYMMETRIC: Clear from users table
                val userId = appPreferences.getUserId()
                if (userId != null) {
                    clearSchoolFromUserEntity(userId)
                }

                Result.Success(true)
            } else {
                Log.e(TAG, "❌ HTTP error: ${response.code()}")
                Result.Error(Exception("Remove mapping failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception: ${e.message}", e)
            Result.Error(e)
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
                Log.d(TAG, "✅ Cleared school info from users table")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to clear school from UserEntity: ${e.message}", e)
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
                Log.d(TAG, "✅ Synced school mapping to users table: $schoolName")
            } else {
                Log.w(TAG, "⚠️ User not found in Room with ID: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to sync school to UserEntity: ${e.message}", e)
        }
    }
}