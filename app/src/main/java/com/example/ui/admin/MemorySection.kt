package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemoryFragment
import com.example.memory.MemoryRetrievalEngine
import com.example.ui.theme.*

@Composable
fun AdminMemoryCard(
    fragments: List<MemoryFragment>,
    isConsolidating: Boolean,
    onConsolidate: () -> Unit,
    onDeleteFragment: (Long) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredFragments = remember(fragments, searchQuery) {
        if (searchQuery.isBlank()) {
            fragments
        } else {
            val q = searchQuery.lowercase().trim()
            fragments.filter {
                it.title.lowercase().contains(q) ||
                it.summary.lowercase().contains(q) ||
                it.tags.lowercase().contains(q)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Hlavička s akcemi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(18.dp))
                    Text(
                        text = "AUTONOMNÍ PAMĚŤ (${fragments.size})",
                        color = OmnisEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Tlačítko pro export JSON-LD
                    if (fragments.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val jsonLd = MemoryRetrievalEngine.exportToJsonLd(fragments)
                                clipboardManager.setText(AnnotatedString(jsonLd))
                                Toast.makeText(context, "Knowledge Graph (JSON-LD) zkopírován do schránky", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON-LD", color = OmnisCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Tlačítko konsolidace
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConsolidate()
                        },
                        enabled = !isConsolidating,
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisEmerald),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        if (isConsolidating) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("KONSOLIDOVAT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vyhledávací pole pro paměťové fragmenty
            if (fragments.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filtrovat fragmenty dle klíčových slov / tagů...", color = OmnisTextMuted, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(14.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Vymazat", tint = OmnisTextMuted, modifier = Modifier.size(12.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisEmerald,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = OmnisEmerald
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Seznam fragmentů
            if (filteredFragments.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisPanelDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Žádné fragmenty neodpovídají filtru '$searchQuery'." else "Žádné paměťové fragmenty nejsou aktivní.",
                            color = OmnisTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredFragments.forEach { fragment ->
                        MemoryFragmentItem(fragment, onDelete = { onDeleteFragment(fragment.id) })
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Hybridní retrieval využívá BM25 TF-IDF scoring, recency decay (časový rozpad e^-λt) a doménové bonusy pro prediktivní injekci kontextu.",
                color = OmnisTextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun MemoryFragmentItem(fragment: MemoryFragment, onDelete: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(14.dp))
                    Text(fragment.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Smazat fragment", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = fragment.summary,
                color = OmnisTextMuted,
                fontSize = 10.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text("Rec: ${fragment.sourceRecordCount}", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                fragment.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                    Surface(color = OmnisEmerald.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                        Text("#${tag.trim()}", color = OmnisEmerald, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}
