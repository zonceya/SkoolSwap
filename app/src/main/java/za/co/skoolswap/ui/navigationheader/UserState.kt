package za.co.skoolswap.ui.navigationheader

// Create this file: ui/navigationheader/UserState.kt
sealed class UserState {
    object Loading : UserState()
    data class Success(
        val name: String?,
        val email: String?,
        val profileImageUrl: String?,
        val localImagePath: String? = null
    ) : UserState()
    data class Error(val message: String) : UserState()
}