package com.example.ui.artifacts

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisArtifact
import com.example.ui.OmnisMarkdownText
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ArtifactGalleryView(
    artifacts: List<OmnisArtifact>,
    onDeleteArtifact: (Long) -> Unit,
    onExportArtifact: (OmnisArtifact) -> Unit,
    onCreateArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null,
    onSendToChat: ((String) -> Unit)? = null
) {
    var selectedArtifact by remember { mutableStateOf<OmnisArtifact?>(null) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val filteredArtifacts = remember(artifacts, selectedTypeFilter, searchQuery) {
        artifacts.filter { artifact ->
            val matchesType = if (selectedTypeFilter == "ALL") true else artifact.type.equals(selectedTypeFilter, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.lowercase().trim()
                artifact.title.lowercase().contains(q) ||
                artifact.language.lowercase().contains(q) ||
                artifact.content.lowercase().contains(q)
            }
            matchesType && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(14.dp)
            .navigationBarsPadding()
            .testTag("artifact_gallery_root")
    ) {
        // Hlavička
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Code, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        "ARTIFACT REPOSITORY",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Autonomně vygenerované modely, skripty a konfigurace (${artifacts.size})",
                        color = OmnisTextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            if (onCreateArtifact != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCreateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nový", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vyhledávací pole
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Hledat v názvu, kódu nebo jazyku...", color = OmnisTextMuted, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(15.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Vymazat", tint = OmnisTextMuted, modifier = Modifier.size(13.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OmnisCyan,
                unfocusedBorderColor = OmnisBorderDark,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = OmnisCyan
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filtrační čipy podle typu
        val types = listOf("ALL", "CODE", "CONFIG", "REPORT", "SCRIPT")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            types.forEach { type ->
                val isSelected = selectedTypeFilter == type
                val chipColor = getArtifactTypeColor(type)

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) chipColor.copy(alpha = 0.2f) else OmnisPanelDark,
                    border = BorderStroke(1.dp, if (isSelected) chipColor else OmnisBorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTypeFilter = type
                        }
                ) {
                    Text(
                        text = type,
                        color = if (isSelected) chipColor else OmnisTextMuted,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredArtifacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = OmnisBorderDark, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedTypeFilter != "ALL") 
                            "Žádné artefakty neodpovídají zadanému filtru." 
                        else 
                            "Žádné artefakty nenalezeny v lokálním repozitáři.", 
                        color = OmnisTextMuted, 
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredArtifacts, key = { it.id }) { artifact ->
                    ArtifactCard(
                        artifact = artifact,
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedArtifact = artifact 
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog && onCreateArtifact != null) {
        CreateArtifactDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { t, typ, l, c ->
                onCreateArtifact(t, typ, l, c)
            }
        )
    }

    selectedArtifact?.let { artifact ->
        ArtifactDetailDialog(
            artifact = artifact,
            onDismiss = { selectedArtifact = null },
            onDelete = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeleteArtifact(artifact.id)
                selectedArtifact = null
            },
            onExport = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onExportArtifact(artifact) 
            },
            onSendToChat = onSendToChat
        )
    }
}

@Composable
fun ArtifactCard(
    artifact: OmnisArtifact,
    onClick: () -> Unit
) {
    val typeColor = getArtifactTypeColor(artifact.type)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = OmnisCardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OmnisBorderDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeColor.copy(alpha = 0.15f))
                        .border(0.5.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getArtifactIcon(artifact.type),
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    color = typeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = artifact.type,
                        color = typeColor,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = artifact.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artifact.language.uppercase(),
                    color = OmnisCyan,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(artifact.timestamp)),
                    color = OmnisTextMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ArtifactDetailDialog(
    artifact: OmnisArtifact,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onSendToChat: ((String) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val typeColor = getArtifactTypeColor(artifact.type)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmnisBgDark,
        titleContentColor = Color.White,
        textContentColor = OmnisTextMuted,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(getArtifactIcon(artifact.type), contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
                Text(
                    text = artifact.title, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OmnisBorderDark)
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(10.dp)
                    ) {
                        if (artifact.type.equals("REPORT", ignoreCase = true)) {
                            OmnisMarkdownText(
                                text = artifact.content
                            )
                        } else {
                            Text(
                                text = artifact.content,
                                color = OmnisCyan.copy(alpha = 0.9f),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Typ: ${artifact.type} | Jazyk: ${artifact.language}",
                        color = OmnisTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Sdílení artefaktu
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TITLE, artifact.title)
                                    putExtra(Intent.EXTRA_TEXT, artifact.content)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Sdílet artefakt: ${artifact.title}")
                                context.startActivity(shareIntent)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, OmnisEmerald),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sdílet", color = OmnisEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Rychlé kopírování kódu artefaktu do schránky
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                clipboardManager.setText(AnnotatedString(artifact.content))
                                Toast.makeText(context, "Obsah artefaktu zkopírován", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, OmnisCyan),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kopírovat", color = OmnisCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onSendToChat != null) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val prompt = "Analyzuj a optimalizuj tento kognitivní artefakt [${artifact.title}] (${artifact.type} / ${artifact.language}):\n\n```${artifact.language}\n${artifact.content}\n```\nNavrhni vylepšení, bezpečnostní audit a možné systémové integrace."
                            onSendToChat(prompt)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Konzultovat", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onExport,
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Smazat", color = Color.Red, fontSize = 11.sp)
                }
                TextButton(onClick = onDismiss) {
                    Text("Zavřít", color = OmnisTextMuted, fontSize = 11.sp)
                }
            }
        }
    )
}

@Composable
fun CreateArtifactDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, type: String, language: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CODE") }
    var language by remember { mutableStateOf("python") }
    var content by remember { mutableStateOf("") }
    val types = listOf("CODE", "CONFIG", "REPORT", "SCRIPT", "PROMPT")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmnisBgDark,
        titleContentColor = Color.White,
        title = {
            Text("Nový znalostní artefakt", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název artefaktu", color = OmnisTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Typ artefaktu:", color = OmnisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { 
                                type = t 
                                when (t) {
                                    "REPORT" -> language = "markdown"
                                    "CONFIG" -> language = "json"
                                    "SCRIPT" -> language = "bash"
                                    "PROMPT" -> language = "text"
                                    else -> language = "python"
                                }
                            },
                            label = { Text(t, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getArtifactTypeColor(t),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Jazyk (python, bash, json, markdown...)", color = OmnisTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Kód, skript nebo obsah...", color = OmnisTextMuted, fontSize = 11.sp) },
                    minLines = 5,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onCreate(title.trim(), type, language.trim(), content.trim())
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Uložit", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = OmnisTextMuted, fontSize = 11.sp)
            }
        }
    )
}

fun getArtifactIcon(type: String) = when(type.uppercase()) {
    "CODE" -> Icons.Default.Code
    "CONFIG" -> Icons.Default.Settings
    "REPORT" -> Icons.Default.Description
    "SCRIPT" -> Icons.Default.Terminal
    else -> Icons.Default.Javascript
}

fun getArtifactTypeColor(type: String) = when(type.uppercase()) {
    "CODE" -> OmnisCyan
    "CONFIG" -> OmnisAmber
    "REPORT" -> OmnisEmerald
    "SCRIPT" -> OmnisViolet
    else -> Color.Gray
}
