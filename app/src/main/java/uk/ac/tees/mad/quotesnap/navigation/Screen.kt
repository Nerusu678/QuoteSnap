package uk.ac.tees.mad.quotesnap.navigation

sealed class Screen(val route:String){

    object SignUpScreen:Screen("sign_up")
    object LoginScreen:Screen("login_in")
    object SplashScreen:Screen("splash")

    object MainScreen:Screen("main")
    object QuoteEditorScreen : Screen("quote_editor/{extractedText}") {
        fun createRoute(extractedText: String) = "quote_editor/$extractedText"
    }
}