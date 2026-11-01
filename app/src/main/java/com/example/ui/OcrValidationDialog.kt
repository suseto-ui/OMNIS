package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.OmnisBgDark
import com.example.ui.theme.OmnisBorderDark
import com.example.ui.theme.OmnisCyan
import com.example.ui.theme.OmnisPanelDark
import com.example.ui.theme.OmnisTextMuted
import java.io.File

@Composable
fun OcrValidationDialog(
    state: OmnisViewModel.OcrValidationState,
    onTextChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var showWarning by remember { mutableStateOf(false) }
    
    // Check if text is meaningful (not just whitespace and has at least some characters)
    val text = state.extractedText.trim()
    val isMeaningful = text.length > 5 && text.any { it.isLetterOrDigit() }
    val isEmpty = text.isEmpty()

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "NÁHLED OCR (OCRPreviewModal)",
                    color = OmnisCyan,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Image Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, OmnisBorderDark, RoundedCornerShape(8.dp))
                        .background(OmnisBgDark),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(state.imageLocalPath),
                        contentDescription = "Analyzovaný obrázek",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Extrahovaný text k úpravě:",
                    color = OmnisTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Extracted Text Field (simulating textarea)
                OutlinedTextField(
                    value = state.extractedText,
                    onValueChange = {
                        onTextChanged(it)
                        showWarning = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedContainerColor = OmnisBgDark,
                        unfocusedContainerColor = OmnisBgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (showWarning) {
                    Text(
                        text = "Varování: Text je prázdný nebo příliš krátký. Opravdu chcete odeslat takto krátký text?",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Zrušit", color = OmnisTextMuted)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (!isMeaningful && !showWarning && !isEmpty) {
                                // First click, short text -> show warning
                                showWarning = true
                            } else if (isEmpty) {
                                // Don't allow empty text submission to avoid bad data
                                showWarning = true
                            } else {
                                // Proceed
                                onConfirm()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan)
                    ) {
                        Text(
                            if (showWarning && !isEmpty) "Ano, přesto odeslat" else "Potvrdit a odeslat", 
                            color = Color.Black, 
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
