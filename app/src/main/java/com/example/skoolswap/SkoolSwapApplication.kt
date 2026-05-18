package com.example.skoolswap


import android.app.Application
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SkoolSwapApplication : Application() {

    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            timber.log.Timber.plant(timber.log.Timber.DebugTree())
        }

        // Initialize DataStore Preferences
        appPreferences = AppPreferences(this)
        FirebaseApp.initializeApp(this)

    }
}
