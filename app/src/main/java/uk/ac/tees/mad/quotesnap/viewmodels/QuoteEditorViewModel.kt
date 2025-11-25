package uk.ac.tees.mad.quotesnap.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.tees.mad.quotesnap.data.QuoteSnapRepository
import uk.ac.tees.mad.quotesnap.data.models.quote.Quote
import javax.inject.Inject

@HiltViewModel
class QuoteEditorViewModel @Inject constructor(
    private val repository: QuoteSnapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuoteEditorUiState())
    val uiState: StateFlow<QuoteEditorUiState> = _uiState.asStateFlow()

    fun initialize(extractedText: String) {
        _uiState.update { it.copy(extractedText = extractedText) }
        fetchQuote(extractedText)
    }

    private fun fetchQuote(extractedText: String) {
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

    // SIMPLE SAVE - Just save design data (no bitmap, no Cloudinary)
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


    // NEW: Share quote function
    fun shareQuote(context: Context) {
        val currentState = _uiState.value

        val shareText = """
            "${currentState.editedQuoteText}"
            
            — ${currentState.editedAuthor}
            
            Created with QuoteSnap 📸
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share Quote")
        )
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