package uk.ac.tees.mad.quotesnap.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.tees.mad.quotesnap.data.ProfileRepository
import uk.ac.tees.mad.quotesnap.data.QuoteSnapRepository
import uk.ac.tees.mad.quotesnap.data.models.quote.Quote
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class QuoteEditorViewModel @Inject constructor(
    private val repository: QuoteSnapRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuoteEditorUiState())
    val uiState: StateFlow<QuoteEditorUiState> = _uiState.asStateFlow()


    // this is getting called in the launched effect
    fun initialize(extractedText: String) {
        _uiState.update { it.copy(extractedText = extractedText) }
        loadUserPreferences()
        fetchQuote(extractedText)
    }

    // ADD THIS NEW FUNCTION
    private fun loadUserPreferences() {
        viewModelScope.launch {
            profileRepository.getUserProfile()
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            backgroundColor = profile.preferences.defaultBackgroundColor,
                            textColor = profile.preferences.defaultTextColor,
                            fontSize = profile.preferences.defaultFontSize
                        )
                    }
                    Log.d("QuoteEditorVM", "Loaded preferences: ${profile.preferences}")
                }
                .onFailure { error ->
                    Log.e("QuoteEditorVM", "Failed to load preferences: ${error.message}")
                    // Keep hardcoded defaults if preferences fail to load
                }
        }
    }



    // this is
    fun fetchQuote(extractedText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuote = true, quoteError = null) }

            repository.getQuoteForText(extractedText)
                .onSuccess { quote ->
                    _uiState.update {
                        it.copy(
                            quote = quote,
                            editedQuoteText = quote.content,
                            editedAuthor = quote.author,
                            isLoadingQuote = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            quoteError = error.message ?: "Failed to fetch quote",
                            isLoadingQuote = false
                        )
                    }
                }
        }
    }

    fun regenerateQuote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuote = true) }
            repository.getRandomQuote()
                .onSuccess { quote ->
                    _uiState.update {
                        it.copy(
                            quote = quote,
                            editedQuoteText = quote.content,
                            editedAuthor = quote.author,
                            isLoadingQuote = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            quoteError = error.message,
                            isLoadingQuote = false
                        )
                    }
                }
        }
    }

    fun updateQuoteText(newText: String) {
        _uiState.update { it.copy(editedQuoteText = newText) }
    }

    fun updateAuthor(newAuthor: String) {
        _uiState.update { it.copy(editedAuthor = newAuthor) }
    }

    fun updateBackgroundColor(colorHex: String) {
        _uiState.update { it.copy(backgroundColor = colorHex) }
    }

    fun updateTextColor(colorHex: String) {
        _uiState.update { it.copy(textColor = colorHex) }
    }

    fun updateFontSize(size: Float) {
        _uiState.update { it.copy(fontSize = size) }
    }

    // SIMPLE SAVE - Just save design data
    fun saveDesignOnly() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }

            val currentState = _uiState.value

            // Call repository method to save design data only
            repository.saveDesignOnly(
                quoteText = currentState.editedQuoteText,
                author = currentState.editedAuthor,
                backgroundColor = currentState.backgroundColor,
                textColor = currentState.textColor,
                fontSize = currentState.fontSize
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            savedSuccessfully = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = error.message ?: "Failed to save"
                        )
                    }
                }
        }
    }


//    // NEW: Share quote function
//    fun shareQuote(context: Context) {
//        val currentState = _uiState.value
//
//        val shareText = """
//            "${currentState.editedQuoteText}"
//
//            — ${currentState.editedAuthor}
//
//            Created with QuoteSnap 📸
//        """.trimIndent()
//
//        val shareIntent = Intent().apply {
//            action = Intent.ACTION_SEND
//            putExtra(Intent.EXTRA_TEXT, shareText)
//            type = "text/plain"
//        }
//
//        context.startActivity(
//            Intent.createChooser(shareIntent, "Share Quote")
//        )
//    }


    fun shareQuote(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val uri = saveBitmapToCache(context, bitmap)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share  Quote Poster"))
            } catch (e: Exception) {
                _uiState.update {
                    Log.d("QuoteES", "shareQuote: ${e.message} ")
                    it.copy(saveError = "Failed to share : ${e.message}")
                }
            }
        }
    }

    private suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        return withContext(Dispatchers.IO) {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "quote_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }


    // ✅ ADD THIS NEW FUNCTION
    fun createPosterBitmap(): Bitmap {
        val currentState = _uiState.value
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Draw background color
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor(currentState.backgroundColor)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Setup text paint for quote
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor(currentState.textColor)
            textSize = currentState.fontSize * 3f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = false
        }

        // Draw quote text with word wrapping
        val quoteText = "\"${currentState.editedQuoteText}\""
        val maxWidth = width - 200f  // Padding on sides
        val lines = wrapText(quoteText, textPaint, maxWidth)

        // Calculate starting Y position (center vertically)
        val lineHeight = textPaint.textSize * 1.4f
        var y = (height / 2f) - ((lines.size * lineHeight) / 2f)

        // Draw each line
        lines.forEach { line ->
            canvas.drawText(line, width / 2f, y, textPaint)
            y += lineHeight
        }

        // Draw author
        val authorPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor(currentState.textColor)
            textSize = currentState.fontSize * 2f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            alpha = (255 * 0.9f).toInt()  // 90% opacity
        }
        canvas.drawText(
            "- ${currentState.editedAuthor}",
            width / 2f,
            y + 80f,
            authorPaint
        )

        return bitmap
    }

    // ✅ ADD THIS HELPER FUNCTION
    private fun wrapText(text: String, paint: android.graphics.Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}

data class QuoteEditorUiState(
    // OCR Text
    val extractedText: String = "",

    // Quote Data
    val quote: Quote? = null,
    val isLoadingQuote: Boolean = false,
    val quoteError: String? = null,

    // Editable Quote Text
    val editedQuoteText: String = "",
    val editedAuthor: String = "",

    // Design Customization
    val backgroundColor: String = "#667eea",  // Default purple gradient
    val textColor: String = "#FFFFFF",        // White text
    val fontSize: Float = 24f,

    // Save State
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedSuccessfully: Boolean = false
)