package uk.ac.tees.mad.quotesnap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.ac.tees.mad.quotesnap.viewmodels.QuoteEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteEditorScreen(
    modifier: Modifier = Modifier,
    extractedText: String,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: QuoteEditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context=LocalContext.current

//    val posterView = LocalView.current
//    var posterBitmap by remember {
//        mutableStateOf<Bitmap?>(null)
//    }
    // Initialize ViewModel with extracted text
    LaunchedEffect(extractedText) {
        viewModel.initialize(extractedText)
    }

    // ✅ NEW: Show success toast
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            Toast.makeText(
                context,
                "✓ Design saved!",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.clearSaveSuccess()
        }
    }

    // ✅ NEW: Show error toast
    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { error ->
            Toast.makeText(
                context,
                "Error: $error",
                Toast.LENGTH_LONG
            ).show()
            // Clear error after showing
            viewModel.clearSaveError()
        }
    }

//    // Navigate back on successful save
//    LaunchedEffect(uiState.savedSuccessfully) {
//        if (uiState.savedSuccessfully) {
//            delay(1500)
//            onSaveSuccess()
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quote Editor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.regenerateQuote() },
                        enabled = !uiState.isLoadingQuote
                    ) {
                        Icon(Icons.Default.Refresh, "New Quote")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .then(
                    if (uiState.isLoadingQuote || uiState.quoteError != null) {
                        Modifier
                    } else {
                        Modifier.verticalScroll(
                            rememberScrollState()
                        )
                    }
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoadingQuote -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Fetching Quote...")
                    }
                }

                uiState.quoteError != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Error: ${uiState.quoteError}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.regenerateQuote() }) {
                            Text("Try Again")
                        }
                    }
                }

                else -> {
//                     Quote Preview
                    PosterPreview(
                        quoteText = uiState.editedQuoteText,
                        author = uiState.editedAuthor,
                        backgroundColor = uiState.backgroundColor,
                        textColor = uiState.textColor,
                        fontSize = uiState.fontSize,
                        fontFamily = uiState.fontFamily
                    )

//                    CapturablePosterPreview(
//                        quoteText = uiState.editedQuoteText,
//                        author = uiState.editedAuthor,
//                        backgroundColor = uiState.backgroundColor,
//                        textColor = uiState.textColor,
//                        fontSize = uiState.fontSize,
//                        onBitmapCaptured = { bitmap ->
//                            posterBitmap = bitmap
//                        }
//                    )

                    // Edit Quote
                    OutlinedTextField(
                        value = uiState.editedQuoteText,
                        onValueChange = { viewModel.updateQuoteText(it) },
                        label = { Text("Quote Text") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // Edit Author
                    OutlinedTextField(
                        value = uiState.editedAuthor,
                        onValueChange = { viewModel.updateAuthor(it) },
                        label = { Text("Author") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Customization
                    CustomizationSection(
                        backgroundColor = uiState.backgroundColor,
                        textColor = uiState.textColor,
                        fontSize = uiState.fontSize,
                        fontFamily = uiState.fontFamily,
                        onBackgroundColorChange = { viewModel.updateBackgroundColor(it) },
                        onTextColorChange = { viewModel.updateTextColor(it) },
                        onFontSizeChange = { viewModel.updateFontSize(it) },
                        onFontFamilyChange = { viewModel.updateFontFamily(it) }
                    )

                    // Save Button - Always available
                    Button(
                        onClick = { viewModel.saveDesignOnly() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && uiState.hasUnsavedChanges()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Text(if (uiState.hasUnsavedChanges()) "Save Design" else "Saved")
                        }
                    }

                    // Share Button - Shows after first successful save
//                    if (uiState.savedSuccessfully) {
                        val context = LocalContext.current

                        OutlinedButton(
                            onClick = {
//                                viewModel.shareQuote(context)
//                                posterBitmap = posterView.drawToBitmap()
//                                posterBitmap?.let {
//                                    viewModel.shareQuote(context, it)
//                                }
                                val bitmap = viewModel.createPosterBitmap()
                                viewModel.shareQuote(context, bitmap)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.hasUnsavedChanges()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.hasUnsavedChanges()) "Save to Share" else "Share Quote")
                        }
//                    }


//                    // Messages
//                    if (uiState.saveError != null) {
//                        Text(
//                            text = "Error: ${uiState.saveError}",
//                            color = MaterialTheme.colorScheme.error
//                        )
//                    }
//                    if (uiState.savedSuccessfully) {
//                        Text(
//                            text = "✓ Design saved!",
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
                }
            }
        }
    }
}


// ✅ ADD THIS ENTIRE NEW FUNCTION after the existing PosterPreview function:

//@Composable
//fun CapturablePosterPreview(
//    quoteText: String,
//    author: String,
//    backgroundColor: String,
//    textColor: String,
//    fontSize: Float,
//    onBitmapCaptured: (Bitmap) -> Unit
//) {
//    val posterView = LocalView.current
//
//    // Capture bitmap whenever content changes
//    LaunchedEffect(quoteText, author, backgroundColor, textColor, fontSize) {
//        // Small delay to ensure UI is fully rendered
//        kotlinx.coroutines.delay(100)
//        try {
//            val bitmap = posterView.drawToBitmap()
//            onBitmapCaptured(bitmap)
//        } catch (e: Exception) {
//            // Handle error silently
//        }
//    }
//
//    // Original PosterPreview
//    PosterPreview(
//        quoteText = quoteText,
//        author = author,
//        backgroundColor = backgroundColor,
//        textColor = textColor,
//        fontSize = fontSize
//    )
//}

@Composable
fun PosterPreview(
    quoteText: String,
    author: String,
    backgroundColor: String,
    textColor: String,
    fontSize: Float,
    fontFamily: String,
) {
    // ADD THIS LOG AT THE TOP
    android.util.Log.d("FontDebug", "PosterPreview - Received fontFamily: $fontFamily")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(backgroundColor))
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,

                ) {
//                val composeFontFamily = when (PosterFont.fromString(fontFamily)) {
////                    PosterFont.SERIF -> FontFamily.Serif
////                    PosterFont.SANS_SERIF -> FontFamily.SansSerif
////                    PosterFont.MONOSPACE -> FontFamily.Monospace
//                    PosterFont.CLASSIC -> FontFamily.Serif
//                    PosterFont.MODERN -> FontFamily.SansSerif
//                    PosterFont.TYPEWRITER -> FontFamily.Monospace
//                }

//                Text(
//                    text = "TEST SANS",
//                    fontFamily = FontFamily.SansSerif,
//                    fontSize = 24.sp
//                )
//                Text(
//                    text = "TEST SERIF",
//                    fontFamily = FontFamily.Serif,
//                    fontSize = 24.sp
//                )
//                Text(
//                    text = "TEST MONO",
//                    fontFamily = FontFamily.Monospace,
//                    fontSize = 24.sp
//                )

                // ADD THIS TEST TEXT - VERY OBVIOUS FONT DIFFERENCES
//                Text(
//                    text = "iIl1 oO0 Testing 123",
//                    fontSize = 32.sp,
//                    color = Color(android.graphics.Color.parseColor(textColor)),
//                    textAlign = TextAlign.Center,
////                    fontFamily = composeFontFamily,
//                    modifier = Modifier.padding(bottom = 16.dp)
//                )
                Text(
                    text = "\"$quoteText\"",
                    fontSize = fontSize.sp,
                    color = Color(android.graphics.Color.parseColor(textColor)),
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize * 1.4f).sp,
//                    fontFamily= composeFontFamily
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "- $author",
                    fontSize = (fontSize * 0.7f).sp,
                    color = Color(android.graphics.Color.parseColor(textColor)).copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
//                    fontFamily=composeFontFamily
                )
            }
        }
    }
}

@Composable
fun CustomizationSection(
    backgroundColor: String,
    textColor: String,
    fontSize: Float,
    fontFamily: String,
    onBackgroundColorChange: (String) -> Unit,
    onTextColorChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit

) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Customize Design", style = MaterialTheme.typography.titleMedium)

            Text("Background Color", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorButton("#667eea", backgroundColor, onBackgroundColorChange)
                ColorButton("#764ba2", backgroundColor, onBackgroundColorChange)
                ColorButton("#f093fb", backgroundColor, onBackgroundColorChange)
                ColorButton("#4facfe", backgroundColor, onBackgroundColorChange)
                ColorButton("#43e97b", backgroundColor, onBackgroundColorChange)
            }

            Text("Text Color", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorButton("#FFFFFF", textColor, onTextColorChange)
                ColorButton("#000000", textColor, onTextColorChange)
                ColorButton("#FFD700", textColor, onTextColorChange)
                ColorButton("#FF6B6B", textColor, onTextColorChange)
            }

//            // NEW: Font Family Selection
//            Text("Font Style", style = MaterialTheme.typography.labelMedium)
//            FontFamilySelector(
//                selectedFont = fontFamily,
//                onFontSelected = onFontFamilyChange
//            )

            Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 16f..26f,
                steps = 5
            )
        }
    }
}

//@Composable
//fun FontFamilySelector(
//    selectedFont: String,
//    onFontSelected: (String) -> Unit
//) {
//    Row(
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        PosterFont.values().forEach { font ->
//            FontButton(
//                font = font,
//                isSelected = font.name == selectedFont,
//                onClick = { onFontSelected(font.name) },
//                modifier=Modifier.weight(1f)
//            )
//        }
//    }
//}

//@Composable
//fun FontButton(
//    font: PosterFont,
//    isSelected: Boolean,
//    onClick: () -> Unit,
//    modifier:Modifier=Modifier,
//) {
//    val composeFontFamily = when (font) {
////        PosterFont.SERIF -> FontFamily.Serif
////        PosterFont.SANS_SERIF -> FontFamily.SansSerif
////        PosterFont.MONOSPACE -> FontFamily.Monospace
//        PosterFont.CLASSIC -> FontFamily.Serif
//        PosterFont.MODERN -> FontFamily.SansSerif
//        PosterFont.TYPEWRITER -> FontFamily.Monospace
//    }
//    OutlinedButton(
//        onClick = onClick,
//        modifier = modifier,
//        colors = ButtonDefaults.outlinedButtonColors(
//            containerColor = if (isSelected) {
//                MaterialTheme.colorScheme.primaryContainer
//            } else {
//                Color.Transparent
//            }
//        ),
//        border = BorderStroke(
//            width = if (isSelected) 2.dp else 1.dp,
//            color = if (isSelected) {
//                MaterialTheme.colorScheme.primary
//            } else {
//                MaterialTheme.colorScheme.outline
//            }
//        )
//    ) {
//        Text(
//            text = font.displayName,
//            style = MaterialTheme.typography.bodySmall,
//            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
//            fontFamily = composeFontFamily,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis
//        )
//    }
//}

@Composable
fun ColorButton(colorHex: String, selectedColor: String, onClick: (String) -> Unit) {
    val isSelected = colorHex == selectedColor
    Button(
        onClick = { onClick(colorHex) },
        modifier = Modifier.size(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(android.graphics.Color.parseColor(colorHex))
        ),
        contentPadding = PaddingValues(0.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {}
}