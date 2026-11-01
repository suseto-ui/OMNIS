package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemoryFragment
import com.example.data.OmnisRecord
import com.example.ui.memory.SemanticMemorySearchHud
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zobrazení dlouhodobé paměti a historie kognitivních záznamů s podporou sémantické RAG mezipaměti.
 */
@Composable
fun MemoryView(
    records: List<OmnisRecord>,
    onItemClick: (OmnisRecord) -> Unit,
    fragments: List<MemoryFragment> = emptyList(),
    onInjectContext: (MemoryFragment) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredRecords = remember(records, searchQuery) {
        if (searchQuery.isBlank()) {
            records
        } else {
            val queryLower = searchQuery.lowercase()
            records.filter {
                it.content.lowercase().contains(queryLower) ||
                it.domain.lowercase().contains(queryLower) ||
                it.cognitiveProcess.lowercase().contains(queryLower)
            }
        }
    }

    val displayFragments = remember(fragments, records) {
        if (fragments.isNotEmpty()) {
            fragments
        } else if (records.isNotEmpty()) {
            records.chunked(3).mapIndexed { idx, chunk ->
                val first = chunk.first()
                MemoryFragment(
                    id = idx.toLong() + 1L,
                    title = "Synapse Fragment #${idx + 1}: ${first.domain}",
                    summary = first.content.take(120) + "...",
                    timestamp = first.timestamp,
                    sourceRecordCount = chunk.size,
                    tags = "${first.domain}, KOGNITIVNÍ, ARCHIV"
                )
            }
        } else {
            emptyList()
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(14.dp)
            .testTag("memory_view")
    ) {
        // Hlavička s vyhledáváním
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(22.dp))
                Column {
                    Text("DLOUHODOBÁ PAMĚŤ & SÉMANTICKÁ MEZIPAMĚŤ", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Celkem ${records.size} záznamů | ${displayFragments.size} RAG fragmentů", color = OmnisTextMuted, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Semantic RAG Memory Search HUD
        SemanticMemorySearchHud(
            fragments = displayFragments,
            onInjectContext = onInjectContext
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "HISTORIE KOGNITIVNÍCH ZÁZNAMŮ (SQLite / Room):",
            color = OmnisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filtrovat syrovou historii...", color = OmnisTextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OmnisCyan,
                unfocusedBorderColor = OmnisBorderDark,
                focusedContainerColor = OmnisPanelDark,
                unfocusedContainerColor = OmnisPanelDark,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Paměť je prázdná" else "Žádné záznamy neodpovídají hledání",
                    color = OmnisTextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
                        border = BorderStroke(1.dp, OmnisBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(record) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = if (record.role == "user") OmnisCyan.copy(alpha = 0.2f) else OmnisEmerald.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = record.role.uppercase(),
                                            color = if (record.role == "user") OmnisCyan else OmnisEmerald,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${record.domain} [${record.cognitiveProcess}]",
                                        color = OmnisTextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(record.timestamp)),
                                    color = OmnisTextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = record.content,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
