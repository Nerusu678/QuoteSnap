package uk.ac.tees.mad.quotesnap.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import uk.ac.tees.mad.quotesnap.viewmodels.QuoteEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteEditorScreen(
    modifier: Modifier = Modifier,
    extractedText: String,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: QuoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize ViewModel with extracted text
    LaunchedEffect(extractedText) {
        viewModel.initialize(extractedText)
    }

//    // Navigate back on successful save
//    LaunchedEffect(uiState.savedSuccessfully) {
//        if (uiState.savedSuccessfully) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

//            // Extracted Text Card
//            Card(modifier = Modifier.fillMaxWidth()) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text("Extracted Text:", style = MaterialTheme.typography.labelLarge)
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = extractedText,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }

            // Loading/Error/Content
            when {
                uiState.isLoadingQuote -> {
                    CircularProgressIndicator()
                    Text("Fetching motivational quote...")
                }

                uiState.quoteError != null -> {
                    Text("Error: ${uiState.quoteError}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.regenerateQuote() }) {
                        Text("Try Again")
                    }
                }

                else -> {
                    // Quote Preview
                    PosterPreview(
                        quoteText = uiState.editedQuoteText,
                        author = uiState.editedAuthor,
                        backgroundColor = uiState.backgroundColor,
                        textColor = uiState.textColor,
                        fontSize = uiState.fontSize
                    )

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
                        onBackgroundColorChange = { viewModel.updateBackgroundColor(it) },
                        onTextColorChange = { viewModel.updateTextColor(it) },
                        onFontSizeChange = { viewModel.updateFontSize(it) }
                    )

                    // Save Button - Always available
                    Button(
                        onClick = { viewModel.saveDesignOnly() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Text("Save Design")
                        }
                    }

// Share Button - Shows after first successful save
                    if (uiState.savedSuccessfully) {
                        val context = androidx.compose.ui.platform.LocalContext.current

                        OutlinedButton(
                            onClick = { viewModel.shareQuote(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Quote")
                        }
                    }


                    // Messages
                    if (uiState.saveError != null) {
                        Text(
                            text = "Error: ${uiState.saveError}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (uiState.savedSuccessfully) {
                        Text(
                            text = "✓ Design saved!",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PosterPreview(
    quoteText: String,
    author: String,
    backgroundColor: String,
    textColor: String,
    fontSize: Float
) {
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
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\"$quoteText\"",
                    fontSize = fontSize.sp,
                    color = Color(android.graphics.Color.parseColor(textColor)),
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize * 1.4f).sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "- $author",
                    fontSize = (fontSize * 0.7f).sp,
                    color = Color(android.graphics.Color.parseColor(textColor)).copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
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
    onBackgroundColorChange: (String) -> Unit,
    onTextColorChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit
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

            Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 16f..40f,
                steps = 11
            )
        }
    }
}

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