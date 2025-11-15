package uk.ac.tees.mad.quotesnap.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun QuoteEditorScreen(
    modifier: Modifier = Modifier,
    extractedText: String,
    onBackClick: () -> Boolean,
    onSaveSuccess:()->Unit
) {
    Scaffold {innerPadding->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Extracted Text : $extractedText")
        }
    }
}