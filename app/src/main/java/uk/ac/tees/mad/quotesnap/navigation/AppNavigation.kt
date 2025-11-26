    package uk.ac.tees.mad.quotesnap.navigation

    import android.util.Log
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.collectAsState
    import androidx.compose.runtime.getValue
    import androidx.compose.ui.Modifier
    import androidx.hilt.navigation.compose.hiltViewModel
    import androidx.navigation.NavType
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import androidx.navigation.navArgument
    import uk.ac.tees.mad.quotesnap.ui.screens.LoginScreen
    import uk.ac.tees.mad.quotesnap.ui.screens.MainScreen
    import uk.ac.tees.mad.quotesnap.ui.screens.QuoteEditorScreen
    import uk.ac.tees.mad.quotesnap.ui.screens.SignUpScreen
    import uk.ac.tees.mad.quotesnap.ui.screens.SplashScreen
    import uk.ac.tees.mad.quotesnap.viewmodels.AuthState
    import uk.ac.tees.mad.quotesnap.viewmodels.AuthViewModel
    import java.net.URLDecoder
    import java.nio.charset.StandardCharsets

    @Composable
    fun AppNavigation(modifier: Modifier = Modifier) {

        val navController = rememberNavController()


        val authViewModel = hiltViewModel<AuthViewModel>()

        NavHost(navController = navController, startDestination = Screen.SplashScreen.route) {
            composable(Screen.SplashScreen.route) {
                SplashScreen(
                    onNavigate = { isLoggedIn ->
                        if (isLoggedIn) {
                            // home screen
                            navController.navigate(Screen.MainScreen.route) {
                                popUpTo(Screen.SplashScreen.route) { inclusive = true }
                            }
                        } else {
                            //login screen
                            navController.navigate(Screen.LoginScreen.route) {
                                popUpTo(Screen.SplashScreen.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.LoginScreen.route) {
                val loginState by authViewModel.loginUiState.collectAsState()
                val authState by authViewModel.authState.collectAsState()

                // Navigate to Home when authenticated
                LaunchedEffect(authState) {
                    if (authState is AuthState.Authenticated) {
                        navController.navigate(Screen.MainScreen.route) {
                            popUpTo(Screen.LoginScreen.route) { inclusive = true }
                        }
                    }
                }
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginClick = { _, _ ->
                        authViewModel.login()
                    },
                    onSignUpClick = {
                        navController.navigate(Screen.SignUpScreen.route)
                    },
                    isLoading = loginState.isLoading,
                    errorMessage = loginState.errorMessage
                )
            }

            composable(Screen.SignUpScreen.route) {

                val signUpState by authViewModel.signUpUiState.collectAsState()
                val authState by authViewModel.authState.collectAsState()
                // Navigate to Home when authenticated
                LaunchedEffect(authState) {
                    if (authState is AuthState.Authenticated) {
                        navController.navigate(Screen.MainScreen.route) {
                            popUpTo(Screen.SignUpScreen.route) { inclusive = true }
                        }
                    }
                }
                SignUpScreen(
                    authViewModel = authViewModel,
                    onSignUpClick = { _, _, _ ->
                        authViewModel.signUp()
                    },
                    onSignInClick = {
                        navController.popBackStack()
                    },
                    isLoading = signUpState.isLoading,
                    errorMessage = signUpState.errorMessage
                )
            }

            composable(Screen.MainScreen.route){
                MainScreen(
                    onNavigateToQuoteEditor = { extractedText ->
                        navController.navigate(
                            Screen.QuoteEditorScreen.createRoute(extractedText)
                        )
                    },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.LoginScreen.route) {
                            popUpTo(Screen.MainScreen.route) { inclusive = true }
                        }
                    }
                )
            }


            composable(
                route = Screen.QuoteEditorScreen.route,
                arguments = listOf(
                    navArgument("extractedText") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedText = backStackEntry.arguments?.getString("extractedText") ?: ""
                val extractedText = URLDecoder.decode(encodedText, StandardCharsets.UTF_8.toString())

                Log.d("AppNavigation", "AppNavigation: $encodedText")
                Log.d("AppNavigation", "AppNavigation: ${URLDecoder.decode(encodedText, StandardCharsets.UTF_8.toString())}")
                QuoteEditorScreen(
                    extractedText = extractedText,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onSaveSuccess = {
                        // Navigate back to main screen and switch to Saved tab
                        navController.popBackStack()
                    }
                )
            }
    //        composable(Screen.CameraScreen.route) {
    //            val cameraViewModel = hiltViewModel<CameraViewModel>()
    //
    //            CameraScreen(
    //                onBackClick = {
    //                    // Optional: Add sign out logic here
    //                    // authViewModel.signOut()
    //                    navController.popBackStack()
    //                },
    //                onProceedToEditor = { extractedText ->
    //                    // Encode the extracted text for navigation
    //                    val encodedText = URLEncoder.encode(
    //                        extractedText,
    //                        StandardCharsets.UTF_8.toString()
    //                    )
    //                    navController.navigate(
    //                        Screen.QuoteEditorScreen.createRoute(encodedText)
    //                    )
    //                },
    //                cameraViewModel = cameraViewModel
    //            )
    //        }


        }

    }