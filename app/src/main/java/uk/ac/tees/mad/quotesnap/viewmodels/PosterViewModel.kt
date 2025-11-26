package uk.ac.tees.mad.quotesnap.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.tees.mad.quotesnap.data.QuoteSnapRepository
import uk.ac.tees.mad.quotesnap.data.local.SavedPoster
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class PosterViewModel @Inject constructor(
    private val quoteRepository: QuoteSnapRepository
) : ViewModel() {

    //Sync on ViewModel creation
    init {
        syncFromFirestore()
    }


    // Get all saved posters from Room database
    val savedPosters: StateFlow<List<SavedPoster>> = quoteRepository.getAllPosters()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Delete a poster
    fun deletePoster(posterId: String) {
        viewModelScope.launch {
            quoteRepository.deletePoster(posterId)
        }
    }

    // Sync from Firestore (optional - for when user logs in on new device)
    fun syncFromFirestore() {
        viewModelScope.launch {
            quoteRepository.syncPostersFromFirestore()
        }
    }


    // for the share button
    // ✅ ADD TO PosterViewModel - Almost identical to QuoteEditorViewModel
    fun sharePoster(context: Context, poster: SavedPoster) {
        viewModelScope.launch {
            try {
                val bitmap = createPosterBitmap(poster)  // ← Only this parameter is different
                val uri = saveBitmapToCache(context, bitmap)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Quote Poster"))
            } catch (e: Exception) {
                Log.e("PosterViewModel", "Share failed: ${e.message}")
            }
        }
    }

    private fun createPosterBitmap(poster: SavedPoster): Bitmap {  // ← Takes SavedPoster instead of using state
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = android.graphics.Color.parseColor(poster.backgroundColor)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Quote text
        val textPaint = Paint().apply {
            color = android.graphics.Color.parseColor(poster.textColor)
            textSize = poster.fontSize * 3f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val quoteText = "\"${poster.quoteText}\""  // ← Direct from poster
        val maxWidth = width - 200f
        val lines = wrapText(quoteText, textPaint, maxWidth)

        val lineHeight = textPaint.textSize * 1.4f
        var y = (height / 2f) - ((lines.size * lineHeight) / 2f)

        lines.forEach { line ->
            canvas.drawText(line, width / 2f, y, textPaint)
            y += lineHeight
        }

        // Author
        val authorPaint = Paint().apply {
            color = android.graphics.Color.parseColor(poster.textColor)
            textSize = poster.fontSize * 2f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            alpha = (255 * 0.9f).toInt()
        }
        canvas.drawText(
            "- ${poster.author}",
            width / 2f,
            y + 80f,
            authorPaint
        )  // ← Direct from poster

        return bitmap
    }

    // ✅ These are IDENTICAL - copy as-is
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    private suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        return withContext(Dispatchers.IO) {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "poster_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }
}