package com.example.skoolswap.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        const val CATEGORY_FILTER_CACHE_PREFIX = "category_filter_cache_"
        const val CATEGORY_FILTER_TIMESTAMP_PREFIX = "category_filter_timestamp_"
    }

    // ==================== Flows ====================

    val isOnboardingFinished: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_FINISHED] ?: false }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[LOGGED_IN] ?: false }
    val isFirstTimeLogin: Flow<Boolean> = context.dataStore.data.map { it[FIRST_TIME_LOGIN] ?: true }

    val schoolMapped: Flow<Boolean> = context.dataStore.data.map { it[SCHOOL_MAPPED] ?: false }
    val schoolId: Flow<Int?> = context.dataStore.data.map { it[SCHOOL_ID] }
    val schoolName: Flow<String?> = context.dataStore.data.map { it[SCHOOL_NAME] }
    val schoolMappingId: Flow<String?> = context.dataStore.data.map { it[SCHOOL_MAPPING_ID] }

    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val userProfileImage: Flow<String?> = context.dataStore.data.map { it[USER_PROFILE_IMAGE] }
    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }

    // ==================== Setters ====================

    suspend fun setOnboardingFinished(finished: Boolean = true) { context.dataStore.edit { it[ONBOARDING_FINISHED] = finished } }
    suspend fun setLoggedIn(loggedIn: Boolean = true) { context.dataStore.edit { it[LOGGED_IN] = loggedIn } }
    suspend fun getLoggedInState(): Boolean = context.dataStore.data.map { it[LOGGED_IN] ?: false }.first()
    suspend fun setFirstTimeLogin(firstTime: Boolean = true) { context.dataStore.edit { it[FIRST_TIME_LOGIN] = firstTime } }

    suspend fun setSchoolMapped(mapped: Boolean) { context.dataStore.edit { it[SCHOOL_MAPPED] = mapped } }
    suspend fun setSchoolInfo(id: Int, name: String) { context.dataStore.edit { it[SCHOOL_ID] = id; it[SCHOOL_NAME] = name } }
    suspend fun setSchoolMappingId(mappingId: String) { context.dataStore.edit { it[SCHOOL_MAPPING_ID] = mappingId } }

    suspend fun setUserId(id: String) { context.dataStore.edit { it[USER_ID] = id } }
    suspend fun setUserId(id: Int) { context.dataStore.edit { it[USER_ID] = id.toString() } }
    suspend fun setUserName(name: String) { context.dataStore.edit { it[USER_NAME] = name } }
    suspend fun setUserEmail(email: String) { context.dataStore.edit { it[USER_EMAIL] = email } }
    suspend fun setUserProfileImage(imageUrl: String) { context.dataStore.edit { it[USER_PROFILE_IMAGE] = imageUrl } }
    suspend fun setAuthToken(token: String) { cachedToken = token; context.dataStore.edit { it[AUTH_TOKEN] = token } }

    fun getAuthTokenSync(): String? {
        cachedToken?.let { return it }
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

    suspend fun clearAuthToken() {
        cachedToken = null
        context.dataStore.edit { it.remove(AUTH_TOKEN) }
    }
    // ==================== Filter Cache ====================

    suspend fun cacheGlobalFilterConfig(configJson: String) {
        context.dataStore.edit {
            it[GLOBAL_FILTER_CONFIG_CACHE] = configJson
            it[GLOBAL_FILTER_CONFIG_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun getCachedGlobalFilterConfig(): String? = context.dataStore.data.map { it[GLOBAL_FILTER_CONFIG_CACHE] }.first()
    suspend fun isGlobalFilterConfigCacheValid(): Boolean {
        val timestamp = context.dataStore.data.map { it[GLOBAL_FILTER_CONFIG_TIMESTAMP] ?: 0L }.first()
        return System.currentTimeMillis() - timestamp < 24 * 60 * 60 * 1000
    }

    suspend fun cacheCategoryFilterConfig(categoryId: Int, configJson: String) {
        context.dataStore.edit {
            it[stringPreferencesKey("${CATEGORY_FILTER_CACHE_PREFIX}$categoryId")] = configJson
            it[longPreferencesKey("${CATEGORY_FILTER_TIMESTAMP_PREFIX}$categoryId")] = System.currentTimeMillis()
        }
    }

    suspend fun getCachedCategoryFilterConfig(categoryId: Int): String? =
        context.dataStore.data.map { it[stringPreferencesKey("${CATEGORY_FILTER_CACHE_PREFIX}$categoryId")] }.first()

    suspend fun isCategoryFilterConfigCacheValid(categoryId: Int): Boolean {
        val timestamp = context.dataStore.data.map { it[longPreferencesKey("${CATEGORY_FILTER_TIMESTAMP_PREFIX}$categoryId")] ?: 0L }.first()
        return System.currentTimeMillis() - timestamp < 60 * 60 * 1000
    }

    // ==================== Helper Methods ====================

    suspend fun hasSchoolMapped(): Boolean = context.dataStore.data.map { it[SCHOOL_MAPPED] ?: false }.first()
    suspend fun getUserId(): Int? = context.dataStore.data.map { it[USER_ID]?.toIntOrNull() }.first()
    suspend fun getSchoolMappingId(): String? = context.dataStore.data.map { it[SCHOOL_MAPPING_ID] }.first()

    // ---- Missing Helpers ----
    fun getSchoolIdSync(): Int? = runBlocking { withTimeoutOrNull(100L) { schoolId.first() } }
    fun getSchoolNameSync(): String? = runBlocking { withTimeoutOrNull(100L) { schoolName.first() } }
    suspend fun getSchoolId(): Int? = schoolId.first()
    suspend fun getSchoolName(): String? = schoolName.first()

    suspend fun clearSchoolInfo() {
        context.dataStore.edit {
            it.remove(SCHOOL_MAPPED)
            it.remove(SCHOOL_ID)
            it.remove(SCHOOL_NAME)
            it.remove(SCHOOL_MAPPING_ID)
        }
    }

    fun clearSchoolInfoSync() { runBlocking { clearSchoolInfo() } }
    fun getSchoolMappingIdSync(): String? = runBlocking { withTimeoutOrNull(100L) { schoolMappingId.first() } }
    fun hasSchoolMappedSync(): Boolean = runBlocking { withTimeoutOrNull(100L) { hasSchoolMapped() } } ?: false
    fun getUserIdSync(): Int? = runBlocking { withTimeoutOrNull(100L) { getUserId() } }

    // ==================== Clear Methods ====================

    suspend fun clearUserData() {
        cachedToken = null
        context.dataStore.edit {
            it.remove(LOGGED_IN)
            it.remove(FIRST_TIME_LOGIN)
            it.remove(USER_ID)
            it.remove(USER_NAME)
            it.remove(USER_EMAIL)
            it.remove(USER_PROFILE_IMAGE)
            it.remove(AUTH_TOKEN)
            it.remove(SCHOOL_MAPPED)
            it.remove(SCHOOL_ID)
            it.remove(SCHOOL_NAME)
            it.remove(SCHOOL_MAPPING_ID)
        }
    }

    suspend fun clearPreferences() { context.dataStore.edit { it.clear() } }
}
