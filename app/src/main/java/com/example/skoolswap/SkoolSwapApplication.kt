package com.example.skoolswap


import android.app.Application
import com.example.skoolswap.data.local.AppPreferences

class SkoolSwapApplication : Application() {

    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()

        // Initialize DataStore Preferences
        appPreferences = AppPreferences(this)
    }
}
