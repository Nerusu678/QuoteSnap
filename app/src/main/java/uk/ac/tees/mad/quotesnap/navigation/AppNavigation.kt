package uk.ac.tees.mad.quotesnap.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uk.ac.tees.mad.quotesnap.ui.screens.HomeScreen
import uk.ac.tees.mad.quotesnap.ui.screens.LoginScreen
import uk.ac.tees.mad.quotesnap.ui.screens.SignUpScreen
import uk.ac.tees.mad.quotesnap.ui.screens.SplashScreen
import uk.ac.tees.mad.quotesnap.viewmodels.AuthState
import uk.ac.tees.mad.quotesnap.viewmodels.AuthViewModel

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
                        navController.navigate(Screen.HomeScreen.route) {
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
                    navController.navigate(Screen.HomeScreen.route) {
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
                    navController.navigate(Screen.HomeScreen.route) {
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

        composable(Screen.HomeScreen.route) {
            HomeScreen()
        }

    }

}