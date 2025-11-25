package com.example.skoolswap.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore delegate
private val Context.dataStore by preferencesDataStore("app_prefs")

class AppPreferences(private val context: Context) {

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
        context.dataStore.edit { it[ONBOARDING_FINISHED] = finished }
    }

    suspend fun setLoggedIn(loggedIn: Boolean = true) {
        context.dataStore.edit { it[LOGGED_IN] = loggedIn }
    }

    suspend fun setFirstTimeLogin(firstTime: Boolean = true) {
        context.dataStore.edit { it[FIRST_TIME_LOGIN] = firstTime }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
