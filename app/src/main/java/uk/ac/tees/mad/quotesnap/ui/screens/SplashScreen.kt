package uk.ac.tees.mad.quotesnap.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Color Palette for Splash Screen
private object SplashColors {
    // Light Theme Gradient Colors
    val LightGradientStart = Color(0xFF6B4CE6)      // Purple
    val LightGradientMiddle = Color(0xFF9D4CE6)     // Magenta
    val LightGradientEnd = Color(0xFFFF6B9D)        // Pink

    // Dark Theme Gradient Colors
    val DarkGradientStart = Color(0xFF2D1B69)       // Dark Purple
    val DarkGradientMiddle = Color(0xFF4A1E69)      // Dark Magenta
    val DarkGradientEnd = Color(0xFF69214F)         // Dark Pink

    // Text Colors
    val LightText = Color.White
    val DarkText = Color.White
}

@Composable
fun SplashScreen(
    onNavigate: (Boolean) -> Unit // true = user logged in, false = not logged in
) {
    val isDark = isSystemInDarkTheme()

    // Animation states
    var logoScale by remember { mutableStateOf(0f) }
    var logoAlpha by remember { mutableStateOf(0f) }
    var appNameAlpha by remember { mutableStateOf(0f) }
    var taglineAlpha by remember { mutableStateOf(0f) }
    var quoteAlpha by remember { mutableStateOf(0f) }

    // Animate logo entrance with spring animation
    val logoScaleAnim = animateFloatAsState(
        targetValue = logoScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val logoAlphaAnim = animateFloatAsState(
        targetValue = logoAlpha,
        animationSpec = tween(durationMillis = 800),
        label = "logo_alpha"
    )

    val appNameAlphaAnim = animateFloatAsState(
        targetValue = appNameAlpha,
        animationSpec = tween(durationMillis = 1000),
        label = "app_name_alpha"
    )

    val taglineAlphaAnim = animateFloatAsState(
        targetValue = taglineAlpha,
        animationSpec = tween(durationMillis = 1000, delayMillis = 200),
        label = "tagline_alpha"
    )

    val quoteAlphaAnim = animateFloatAsState(
        targetValue = quoteAlpha,
        animationSpec = tween(durationMillis = 1200, delayMillis = 400),
        label = "quote_alpha"
    )

    // Infinite gradient animation
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedGradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    // Select gradient colors based on theme
    val gradientColors = if (isDark) {
        listOf(
            SplashColors.DarkGradientStart,
            SplashColors.DarkGradientMiddle,
            SplashColors.DarkGradientEnd,
            SplashColors.DarkGradientMiddle,
            SplashColors.DarkGradientStart
        )
    } else {
        listOf(
            SplashColors.LightGradientStart,
            SplashColors.LightGradientMiddle,
            SplashColors.LightGradientEnd,
            SplashColors.LightGradientMiddle,
            SplashColors.LightGradientStart
        )
    }

    // Launch animations and navigation
    LaunchedEffect(Unit) {
        // Logo animation
        delay(300)
        logoScale = 1f
        logoAlpha = 1f

        // App name animation
        delay(500)
        appNameAlpha = 1f

        // Tagline animation
        delay(200)
        taglineAlpha = 1f

        // Quote animation
        delay(500)
        quoteAlpha = 1f

        // Check authentication and navigate
        delay(2500)
        // TODO: Check Firebase Authentication status
        // val currentUser = FirebaseAuth.getInstance().currentUser
        // val isLoggedIn = currentUser != null
        val isLoggedIn = false // Replace with actual Firebase check
        onNavigate(isLoggedIn)
    }

    // Animated gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, animatedGradientOffset * 1000),
                    end = Offset(1000f, 1000f + animatedGradientOffset * 1000)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // App Logo/Icon with animated circle background
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScaleAnim.value)
                    .alpha(logoAlphaAnim.value)
                    .background(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Camera emoji as logo
                Text(
                    text = "📸",
                    fontSize = 70.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "QuoteSnap",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = SplashColors.LightText,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(appNameAlphaAnim.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "Capture. Inspire. Share.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = SplashColors.LightText.copy(alpha = 0.9f),
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(taglineAlphaAnim.value)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Inspirational Loading Quote
            Text(
                text = "\"Every picture tells a story,\nevery quote sparks inspiration.\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = SplashColors.LightText.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier
                    .alpha(quoteAlphaAnim.value)
                    .padding(horizontal = 24.dp)
            )
        }

        // Loading indicator at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LoadingDots(
                modifier = Modifier.alpha(quoteAlphaAnim.value)
            )
        }
    }
}

@Composable
private fun LoadingDots(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    // Animate three dots with staggered delays
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoadingDot(alpha = dot1Alpha)
        LoadingDot(alpha = dot2Alpha)
        LoadingDot(alpha = dot3Alpha)
    }
}

@Composable
private fun LoadingDot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .alpha(alpha)
            .background(
                color = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

// Preview functions for Light and Dark modes
@Preview(
    name = "Splash Screen - Light Mode",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun SplashScreenLightPreview() {
    MaterialTheme {
        Surface {
            SplashScreenPreview(isDarkMode = false)
        }
    }
}

@Preview(
    name = "Splash Screen - Dark Mode",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SplashScreenDarkPreview() {
    MaterialTheme {
        Surface {
            SplashScreenPreview(isDarkMode = true)
        }
    }
}

// Preview composable that shows the splash screen in final animated state
@Composable
private fun SplashScreenPreview(isDarkMode: Boolean) {
    // Infinite gradient animation
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedGradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    // Select gradient colors based on theme
    val gradientColors = if (isDarkMode) {
        listOf(
            SplashColors.DarkGradientStart,
            SplashColors.DarkGradientMiddle,
            SplashColors.DarkGradientEnd,
            SplashColors.DarkGradientMiddle,
            SplashColors.DarkGradientStart
        )
    } else {
        listOf(
            SplashColors.LightGradientStart,
            SplashColors.LightGradientMiddle,
            SplashColors.LightGradientEnd,
            SplashColors.LightGradientMiddle,
            SplashColors.LightGradientStart
        )
    }

    // Animated gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, animatedGradientOffset * 1000),
                    end = Offset(1000f, 1000f + animatedGradientOffset * 1000)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // App Logo/Icon with circle background - showing final state
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Camera emoji as logo
                Text(
                    text = "📸",
                    fontSize = 70.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "QuoteSnap",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = SplashColors.LightText,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "Capture. Inspire. Share.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = SplashColors.LightText.copy(alpha = 0.9f),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Inspirational Loading Quote
            Text(
                text = "\"Every picture tells a story,\nevery quote sparks inspiration.\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = SplashColors.LightText.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Loading indicator at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LoadingDots()
        }
    }
}