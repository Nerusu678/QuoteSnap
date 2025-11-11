package uk.ac.tees.mad.quotesnap.navigation

sealed class Screen(val route:String){

    object SignUpScreen:Screen("sign_up")
    object LoginScreen:Screen("login_in")
    object HomeScreen:Screen("home")
    object SplashScreen:Screen("splash")
}