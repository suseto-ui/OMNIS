package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisSemanticRecord
import com.example.data.SemanticAnchor
import com.example.ui.theme.*

@Composable
fun TestSemanticChatView(
    records: List<OmnisSemanticRecord>,
    isLoading: Boolean,
    inputQuery: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    onWeightChange: (Long, String, String, Float) -> Unit,
    onReSynthesize: (Long, String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(records) { record ->
                TestSemanticRecordItem(
                    record = record,
                    onWeightChange = onWeightChange,
                    onReSynthesize = onReSynthesize
                )
            }
        }

        // Input
        Surface(
            color = OmnisPanelDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                    placeholder = if (!isFocused) {
                        { Text("Zadejte prompt: [Kognitivní doména] + [Akce] + [Kritérium]...", color = OmnisTextMuted, fontSize = 12.sp) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = OmnisCyan,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Odeslat", tint = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun TestSemanticRecordItem(
    record: OmnisSemanticRecord,
    onWeightChange: (Long, String, String, Float) -> Unit,
    onReSynthesize: (Long, String) -> Unit
) {
    val isUser = record.role == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) OmnisViolet.copy(alpha = 0.15f) else OmnisPanelDark
    val borderColor = if (isUser) OmnisViolet.copy(alpha = 0.5f) else OmnisBorderDark

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isUser) {
                    Text(
                        text = record.content,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    com.example.ui.OmnisMarkdownText(
                        text = record.content,
                        textColor = Color.White,
                        fontSize = 14
                    )
                }
                
                if (!isUser && record.anchors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("SÉMANTICKÉ KOTVY (TEST)", color = OmnisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    
                    record.anchors.forEach { (domain, anchors) ->
                        Text(domain.uppercase(), color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        anchors.forEach { anchor ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(anchor.word, color = OmnisTextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Slider(
                                    value = anchor.weight,
                                    onValueChange = { onWeightChange(record.id, domain, anchor.word, it) },
                                    modifier = Modifier.weight(1.5f),
                                    colors = SliderDefaults.colors(thumbColor = OmnisCyan, activeTrackColor = OmnisCyan)
                                )
                                Text("${(anchor.weight * 100).toInt()}%", color = Color.White, fontSize = 10.sp, modifier = Modifier.width(36.dp))
                            }
                        }
                        
                        Button(
                            onClick = { onReSynthesize(record.id, domain) },
                            colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("RE-SYNTÉZA KONTEXTU", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
