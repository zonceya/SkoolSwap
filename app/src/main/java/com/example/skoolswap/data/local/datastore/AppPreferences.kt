package com.example.skoolswap.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var cachedToken: String? = null
    companion object {
        // Boolean preferences
        val ONBOARDING_FINISHED = booleanPreferencesKey("onboarding_finished")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val FIRST_TIME_LOGIN = booleanPreferencesKey("first_time_login")

        // School-related preferences
        val SCHOOL_MAPPED = booleanPreferencesKey("school_mapped")
        val SCHOOL_ID = intPreferencesKey("school_id")
        val SCHOOL_NAME = stringPreferencesKey("school_name")
        val SCHOOL_MAPPING_ID = stringPreferencesKey("school_mapping_id")

        // String preferences for user data
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PROFILE_IMAGE = stringPreferencesKey("user_profile_image")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")

        // Filter cache keys
        val GLOBAL_FILTER_CONFIG_CACHE = stringPreferencesKey("global_filter_config_cache")
        val GLOBAL_FILTER_CONFIG_TIMESTAMP = longPreferencesKey("global_filter_config_timestamp")

        // Category-specific filter cache keys
        val CATEGORY_FILTER_CACHE_PREFIX = "category_filter_cache_"
        val CATEGORY_FILTER_TIMESTAMP_PREFIX = "category_filter_timestamp_"
    }

    // ==================== Existing Preferences ====================

    val isOnboardingFinished: Flow<Boolean> = context.dataStore.data
        .map { it[ONBOARDING_FINISHED] ?: false }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { it[LOGGED_IN] ?: false }

    val isFirstTimeLogin: Flow<Boolean> = context.dataStore.data
        .map { it[FIRST_TIME_LOGIN] ?: true }

    // School-related flows
    val schoolMapped: Flow<Boolean> = context.dataStore.data
        .map { it[SCHOOL_MAPPED] ?: false }

    val schoolId: Flow<Int?> = context.dataStore.data
        .map { it[SCHOOL_ID] }

    val schoolName: Flow<String?> = context.dataStore.data
        .map { it[SCHOOL_NAME] }

    val schoolMappingId: Flow<String?> = context.dataStore.data
        .map { it[SCHOOL_MAPPING_ID] }

    // User data getters
    val userId: Flow<String?> = context.dataStore.data
        .map { it[USER_ID] }

    val userName: Flow<String?> = context.dataStore.data
        .map { it[USER_NAME] }

    val userEmail: Flow<String?> = context.dataStore.data
        .map { it[USER_EMAIL] }

    val userProfileImage: Flow<String?> = context.dataStore.data
        .map { it[USER_PROFILE_IMAGE] }

    val authToken: Flow<String?> = context.dataStore.data
        .map { it[AUTH_TOKEN] }

    // ==================== Existing Setters ====================

    suspend fun setOnboardingFinished(finished: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_FINISHED] = finished
        }
    }

    suspend fun setLoggedIn(loggedIn: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[LOGGED_IN] = loggedIn
        }
    }

    suspend fun getLoggedInState(): Boolean {
        return context.dataStore.data.map { it[LOGGED_IN] ?: false }.first()
    }

    suspend fun setFirstTimeLogin(firstTime: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_TIME_LOGIN] = firstTime
        }
    }

    // School-related setters
    suspend fun setSchoolMapped(mapped: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SCHOOL_MAPPED] = mapped
        }
    }

    suspend fun setSchoolInfo(id: Int, name: String) {
        context.dataStore.edit { preferences ->
            preferences[SCHOOL_ID] = id
            preferences[SCHOOL_NAME] = name
        }
    }

    suspend fun setSchoolMappingId(mappingId: String) {
        context.dataStore.edit { preferences ->
            preferences[SCHOOL_MAPPING_ID] = mappingId
        }
    }

    // User data setters
    suspend fun setUserId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = id
        }
    }

    suspend fun setUserId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = id.toString()
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun setUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = email
        }
    }

    suspend fun setUserProfileImage(imageUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PROFILE_IMAGE] = imageUrl
        }
    }

    suspend fun setAuthToken(token: String) {
        cachedToken = token
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }
    fun getAuthTokenSync(): String? {
        // If we have cached token, return it immediately
        cachedToken?.let { return it }

        // Try to load from DataStore with timeout (max 100ms to avoid ANR)
        return try {
            runBlocking {
                withTimeoutOrNull(100L) {
                    val token = authToken.first()
                    cachedToken = token
                    token
                }
            }
        } catch (e: Exception) {
            Timber.tag("AppPreferences").e(e, "Failed to get token sync")
            null
        }
    }
    // ==================== Filter Cache Methods ====================

    /**
     * Cache global filter config (categories, conditions, etc.)
     * Valid for 24 hours
     */
    suspend fun cacheGlobalFilterConfig(configJson: String) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_FILTER_CONFIG_CACHE] = configJson
            preferences[GLOBAL_FILTER_CONFIG_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Get cached global filter config JSON
     */
    suspend fun getCachedGlobalFilterConfig(): String? {
        return context.dataStore.data.map { it[GLOBAL_FILTER_CONFIG_CACHE] }.first()
    }

    /**
     * Check if global filter config cache is still valid (less than 24 hours old)
     */
    suspend fun isGlobalFilterConfigCacheValid(): Boolean {
        val timestamp = context.dataStore.data.map { it[GLOBAL_FILTER_CONFIG_TIMESTAMP] ?: 0L }.first()
        // Cache valid for 24 hours
        return System.currentTimeMillis() - timestamp < 24 * 60 * 60 * 1000
    }

    /**
     * Cache category-specific filter config (e.g., Uniforms filters)
     * Valid for 1 hour
     */
    suspend fun cacheCategoryFilterConfig(categoryId: Int, configJson: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("${CATEGORY_FILTER_CACHE_PREFIX}$categoryId")] = configJson
            preferences[longPreferencesKey("${CATEGORY_FILTER_TIMESTAMP_PREFIX}$categoryId")] = System.currentTimeMillis()
        }
    }

    /**
     * Get cached category-specific filter config JSON
     */
    suspend fun getCachedCategoryFilterConfig(categoryId: Int): String? {
        return context.dataStore.data.map { it[stringPreferencesKey("${CATEGORY_FILTER_CACHE_PREFIX}$categoryId")] }.first()
    }


    /**
     * Check if category-specific filter config cache is still valid (less than 1 hour old)
     */
    suspend fun isCategoryFilterConfigCacheValid(categoryId: Int): Boolean {
        val timestamp = context.dataStore.data.map { it[longPreferencesKey("${CATEGORY_FILTER_TIMESTAMP_PREFIX}$categoryId")] ?: 0L }.first()
        // Cache valid for 1 hour (categories can change more frequently)
        return System.currentTimeMillis() - timestamp < 60 * 60 * 1000
    }

    /**
     * Clear all filter caches (useful for refresh or logout)
     */
    /*suspend fun clearFilterCaches() {
        context.dataStore.edit { preferences ->
            preferences.remove(GLOBAL_FILTER_CONFIG_CACHE)
            preferences.remove(GLOBAL_FILTER_CONFIG_TIMESTAMP)
            // Remove all category-specific caches
            val allKeys = preferences.asMap().keys
            allKeys.forEach { key ->
                if (key.startsWith(CATEGORY_FILTER_CACHE_PREFIX) || key.startsWith(CATEGORY_FILTER_TIMESTAMP_PREFIX)) {
                    preferences.remove(key)
                }
            }
        }
    }*/

    // ==================== Helper Methods ====================

    /**
     * Helper to check school status
     */
    suspend fun hasSchoolMapped(): Boolean {
        return context.dataStore.data.map { it[SCHOOL_MAPPED] ?: false }.first()
    }

    /**
     * Get user ID as Int
     */
    suspend fun getUserId(): Int? {
        return context.dataStore.data.map { it[USER_ID]?.toIntOrNull() }.first()
    }

    /**
     * Get school mapping ID
     */
    suspend fun getSchoolMappingId(): String? {
        return context.dataStore.data.map { it[SCHOOL_MAPPING_ID] }.first()
    }

    // ==================== Clear Methods ====================

    /**
     * Clear all user data (for logout/delete)
     */
    suspend fun clearUserData() {
        cachedToken = null
        context.dataStore.edit { preferences ->
            preferences.remove(LOGGED_IN)
            preferences.remove(FIRST_TIME_LOGIN)
            preferences.remove(USER_ID)
            preferences.remove(USER_NAME)
            preferences.remove(USER_EMAIL)
            preferences.remove(USER_PROFILE_IMAGE)
            preferences.remove(AUTH_TOKEN)
            preferences.remove(SCHOOL_MAPPED)
            preferences.remove(SCHOOL_ID)
            preferences.remove(SCHOOL_NAME)
            preferences.remove(SCHOOL_MAPPING_ID)
        }
    }

    /**
     * Clear all preferences (use with caution)
     */
    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}