package com.example.skoolswap.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // Boolean preferences
        val ONBOARDING_FINISHED = booleanPreferencesKey("onboarding_finished")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val FIRST_TIME_LOGIN = booleanPreferencesKey("first_time_login")

        // School-related preferences
        val SCHOOL_MAPPED = booleanPreferencesKey("school_mapped")
        val SCHOOL_ID = intPreferencesKey("school_id")
        val SCHOOL_NAME = stringPreferencesKey("school_name")
        val SCHOOL_MAPPING_ID = stringPreferencesKey("school_mapping_id")  // ← ADD THIS

        // String preferences for user data
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PROFILE_IMAGE = stringPreferencesKey("user_profile_image")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    // Existing preferences
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

    val schoolMappingId: Flow<String?> = context.dataStore.data  // ← ADD THIS
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

    // Existing setters
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

    suspend fun setSchoolMappingId(mappingId: String) {  // ← ADD THIS
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
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }

    // Helper to check school status
    suspend fun hasSchoolMapped(): Boolean {
        return context.dataStore.data.map { it[SCHOOL_MAPPED] ?: false }.first()
    }

    // Get user ID as Int
    suspend fun getUserId(): Int? {
        return context.dataStore.data.map { it[USER_ID]?.toIntOrNull() }.first()
    }

    // Get school mapping ID
    suspend fun getSchoolMappingId(): String? {  // ← ADD THIS
        return context.dataStore.data.map { it[SCHOOL_MAPPING_ID] }.first()
    }

    // Clear all user data (for logout/delete)
    suspend fun clearUserData() {
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

    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}