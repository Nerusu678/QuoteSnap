package uk.ac.tees.mad.quotesnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import uk.ac.tees.mad.quotesnap.navigation.AppNavigation
import uk.ac.tees.mad.quotesnap.ui.screens.LoginScreen
import uk.ac.tees.mad.quotesnap.ui.screens.SplashScreen
import uk.ac.tees.mad.quotesnap.ui.theme.QuoteSnapTheme


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuoteSnapTheme {

                AppNavigation()
//                AppNavigation()
//                SplashScreen {  }
//                LoginScreen()
            }
        }
    }
}

