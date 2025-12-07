package com.example.sekeni.ui.profile

import androidx.lifecycle.ViewModel

class ProfileViewModel  : ViewModel() {
    var userName: String? = null
    var userProfilePicUrl: String? = null

    fun updateUserProfile(name: String, profilePicUrl: String) {
        userName = name
        userProfilePicUrl = profilePicUrl
    }
}