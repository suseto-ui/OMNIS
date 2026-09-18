package com.example.ui.artifacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisArtifact
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ArtifactGalleryView(
    artifacts: List<OmnisArtifact>,
    onDeleteArtifact: (Long) -> Unit,
    onExportArtifact: (OmnisArtifact) -> Unit
) {
    var selectedArtifact by remember { mutableStateOf<OmnisArtifact?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp)
            .navigationBarsPadding()
            .testTag("artifact_gallery_root")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Code, contentDescription = null, tint = OmnisCyan)
            Text(
                "ARTIFACT REPOSITORY",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Text(
            "Správa autonomně generovaných výstupů a systémových konfigurací.",
            color = OmnisTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (artifacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = OmnisBorderDark, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Žádné artefakty nenalezeny", color = OmnisTextMuted, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(artifacts) { artifact ->
                    ArtifactCard(
                        artifact = artifact,
                        onClick = { selectedArtifact = artifact }
                    )
                }
            }
        }
    }

    selectedArtifact?.let { artifact ->
        ArtifactDetailDialog(
            artifact = artifact,
            onDismiss = { selectedArtifact = null },
            onDelete = { 
                onDeleteArtifact(artifact.id)
                selectedArtifact = null
            },
            onExport = { onExportArtifact(artifact) }
        )
    }
}

@Composable
fun ArtifactCard(
    artifact: OmnisArtifact,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        color = OmnisCardDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(getArtifactTypeColor(artifact.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getArtifactIcon(artifact.type),
                    contentDescription = null,
                    tint = getArtifactTypeColor(artifact.type),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = artifact.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                fontFamily = FontFamily.Monospace
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
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(artifact.timestamp)),
                    color = OmnisTextMuted,
                    fontSize = 9.sp
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
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmnisBgDark,
        titleContentColor = Color.White,
        textContentColor = OmnisTextMuted,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(getArtifactIcon(artifact.type), contentDescription = null, tint = OmnisCyan)
                Text(artifact.title, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
                ) {
                    androidx.compose.foundation.rememberScrollState().let { scrollState ->
                        Column(
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = artifact.content,
                                color = OmnisCyan.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Typ: ${artifact.type} | Jazyk: ${artifact.language}",
                    color = OmnisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Smazat", color = Color.Red)
            }
        }
    )
}

fun getArtifactIcon(type: String) = when(type) {
    "CODE" -> Icons.Default.Code
    "CONFIG" -> Icons.Default.Settings
    "REPORT" -> Icons.Default.Description
    "SCRIPT" -> Icons.Default.Terminal
    else -> Icons.Default.Javascript
}

fun getArtifactTypeColor(type: String) = when(type) {
    "CODE" -> OmnisCyan
    "CONFIG" -> OmnisAmber
    "REPORT" -> OmnisEmerald
    "SCRIPT" -> OmnisViolet
    else -> Color.Gray
}
