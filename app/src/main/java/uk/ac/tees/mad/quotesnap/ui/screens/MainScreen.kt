package uk.ac.tees.mad.quotesnap.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun MainScreen(
    onNavigateToQuoteEditor: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomNavController = rememberNavController()
    // State to control bottom bar visibility
    var showBottomBar by remember { mutableStateOf(true) }
    Scaffold(
        bottomBar = {
            if(showBottomBar){
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
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
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
            }        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavScreen.Camera.route,
            modifier = Modifier.padding(if (showBottomBar) paddingValues else PaddingValues())

        ) {
            composable(
                route = BottomNavScreen.Camera.route
            ) {
                val cameraViewModel = hiltViewModel<CameraViewModel>()
                CameraScreen(
                    onBackClick = { /* Optional: Handle back if needed */ },
                    onProceedToEditor = { extractedText ->
                        val encodedText = URLEncoder.encode(
                            extractedText,
                            StandardCharsets.UTF_8.toString()
                        )
                        onNavigateToQuoteEditor(encodedText)
                    },
                    cameraViewModel = cameraViewModel,
                    onBottomBarVisibilityChange = {
                        showBottomBar=it
                    }
                )
            }

            composable(
                route = BottomNavScreen.Posters.route
            ) {
                LaunchedEffect(Unit) {
                    showBottomBar = true
                }
                PosterScreen()
            }
            composable(
                route = BottomNavScreen.Profile.route
            ) {
                LaunchedEffect(Unit) {
                    showBottomBar = true
                }
                ProfileScreen(

                )
            }
        }
    }

}


