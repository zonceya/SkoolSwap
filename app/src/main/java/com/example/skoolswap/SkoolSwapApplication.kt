package com.example.skoolswap


import android.app.Application
import com.example.skoolswap.data.local.AppPreferences
import com.google.firebase.FirebaseApp

class SkoolSwapApplication : Application() {

    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()

        // Initialize DataStore Preferences
        appPreferences = AppPreferences(this)
       // FirebaseApp.initialize(this)
    }
}
