// data/local/AppPreferences.kt
package com.example.skoolswap.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        val ONBOARDING_FINISHED = booleanPreferencesKey("onboarding_finished")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val FIRST_TIME_LOGIN = booleanPreferencesKey("first_time_login")
    }

    val isOnboardingFinished: Flow<Boolean> = context.dataStore.data
        .map { it[ONBOARDING_FINISHED] ?: false }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { it[LOGGED_IN] ?: false }

    val isFirstTimeLogin: Flow<Boolean> = context.dataStore.data
        .map { it[FIRST_TIME_LOGIN] ?: true }

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

    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}