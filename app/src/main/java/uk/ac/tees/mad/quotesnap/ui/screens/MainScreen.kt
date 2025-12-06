package uk.ac.tees.mad.quotesnap.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uk.ac.tees.mad.quotesnap.navigation.bottom_navigation.BottomNavScreen
import uk.ac.tees.mad.quotesnap.navigation.bottom_navigation.bottomNavScreens
import uk.ac.tees.mad.quotesnap.ui.screens.bottom_screen.CameraScreen
import uk.ac.tees.mad.quotesnap.ui.screens.bottom_screen.PosterScreen
import uk.ac.tees.mad.quotesnap.ui.screens.bottom_screen.ProfileScreen
import uk.ac.tees.mad.quotesnap.viewmodels.CameraViewModel
import uk.ac.tees.mad.quotesnap.viewmodels.PosterViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun MainScreen(
    onNavigateToQuoteEditor: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomNavController = rememberNavController()
    val posterViewModel = hiltViewModel<PosterViewModel>()
    val cameraScreenViewModel = hiltViewModel<CameraViewModel>()


    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry = bottomNavController.currentBackStackEntryAsState()
                val currentRoute =
                    navBackStackEntry.value?.destination?.route ?: BottomNavScreen.Camera.route
                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = {
                            Text(screen.title)
                        },
                        icon = {
                            Icon(screen.icon, contentDescription = screen.title)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavScreen.Camera.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(route = BottomNavScreen.Camera.route) {
                CameraScreen(
                    onBackClick = { /* Optional: Handle back if needed */ },
                    onProceedToEditor = { extractedText ->
                        val encodedText = URLEncoder.encode(
                            extractedText,
                            StandardCharsets.UTF_8.toString()
                        )
                        onNavigateToQuoteEditor(encodedText)
                    },
                    cameraViewModel = cameraScreenViewModel
                )
            }

            composable(route = BottomNavScreen.Posters.route) {
                PosterScreen(
                    viewModel = posterViewModel
                )
            }

            composable(route = BottomNavScreen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        // go to the login screen and also delete the db
                        onLogout()
                    }
                )
            }
        }
    }
}