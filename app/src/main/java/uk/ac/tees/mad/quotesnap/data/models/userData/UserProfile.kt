package uk.ac.tees.mad.quotesnap.data.models.userData

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val preferences: UserPreferences = UserPreferences()
)


data class UserPreferences(
    val defaultFontSize: Float = 24f,
    val defaultTextColor: String = "#FFFFFF",
    val defaultBackgroundColor: String = "#667eea",
    val theme: String = "light", // "light" or "dark"
    val autoSyncEnabled: Boolean = true
)

