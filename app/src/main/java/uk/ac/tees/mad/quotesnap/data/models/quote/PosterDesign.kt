package uk.ac.tees.mad.quotesnap.data.models.quote

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

data class PosterDesign(
    val quoteText: String = "",
    val author: String = "",
    val backgroundColor: Long = 0xFF667eea, // Store as Long for Room
    val textColor: Long = 0xFFFFFFFF,
    val fontSize: Float = 24f,
    val fontFamily: String = "Default",
    val textAlign: String = "Center" // Store as String for Room
)

// Extension functions for easy conversion
fun PosterDesign.getBackgroundColor() = Color(backgroundColor)
fun PosterDesign.getTextColor() = Color(textColor)
fun PosterDesign.getTextAlignment() = when (textAlign) {
    "Left" -> TextAlign.Left
    "Right" -> TextAlign.Right
    else -> TextAlign.Center
}