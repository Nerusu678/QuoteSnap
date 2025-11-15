package uk.ac.tees.mad.quotesnap.navigation.bottom_navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavScreen(val route: String, val title: String, val icon: ImageVector) {
    object Camera : BottomNavScreen("camera_tab", "Camera", Icons.Default.CameraAlt)
    object Posters : BottomNavScreen("posters_tab", "Saved", Icons.Default.Bookmark)
    object Profile : BottomNavScreen("profile_tab", "Profile", Icons.Default.Person)
}

val bottomNavScreens = listOf(
    BottomNavScreen.Camera,
    BottomNavScreen.Posters,
    BottomNavScreen.Profile
)