package com.example.ui.dialectics

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dialectics.DialecticEngine
import com.example.dialectics.DialecticPerspective
import com.example.dialectics.DialecticSynthesis
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DialecticEngineView(
    onSendToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var inputThesis by remember { mutableStateOf(DialecticEngine.standardDebatePresets.first()) }
    var isDeliberating by remember { mutableStateOf(false) }
    var currentSynthesis by remember { mutableStateOf<DialecticSynthesis?>(null) }
    var selectedPresetIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (currentSynthesis == null) {
            isDeliberating = true
            currentSynthesis = DialecticEngine.conductDialecticDebate(inputThesis)
            isDeliberating = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OmnisCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Dialectic Engine",
                                tint = OmnisCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "EPISTEMICKÝ DIALEKTICKÝ ENGINE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Víceagentní deliberace • Teze vs. Antiteze • Syntéza",
                                color = OmnisTextLight,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Preset selector
        item {
            Text(
                text = "PŘEDNASTAVENÉ SYSTÉMOVÉ DILEMA",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = OmnisCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialecticEngine.standardDebatePresets.forEachIndexed { index, preset ->
                    val isSelected = selectedPresetIndex == index
                    Surface(
                        color = if (isSelected) OmnisCyan.copy(alpha = 0.12f) else OmnisPanelDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) OmnisCyan else OmnisBorderDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPresetIndex = index
                                inputThesis = preset
                                coroutineScope.launch {
                                    isDeliberating = true
                                    currentSynthesis = DialecticEngine.conductDialecticDebate(preset)
                                    isDeliberating = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = OmnisCyan)
                            )
                            Text(
                                text = preset,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else OmnisTextLight,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Custom Thesis input
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "VLASTNÍ TEZE K OPOZITNÍ ANALÝZE:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = OmnisTextLight,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputThesis,
                        onValueChange = { inputThesis = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmnisCyan,
                            unfocusedBorderColor = OmnisBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isDeliberating = true
                                currentSynthesis = DialecticEngine.conductDialecticDebate(inputThesis)
                                isDeliberating = false
                            }
                        },
                        enabled = !isDeliberating && inputThesis.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OmnisCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDeliberating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PROBÍHÁ MULTI-AGENTNÍ DEBATA...", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SPUSTIT DIALEKTICKOU SYNTÉZU", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Metrics Banner
        currentSynthesis?.let { synth ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem(
                            label = "Konsensus",
                            value = "${(synth.consensusIndex * 100).toInt()}%",
                            color = OmnisCyan
                        )
                        MetricItem(
                            label = "Nejistota",
                            value = "${(synth.epistemicUncertainty * 100).toInt()}%",
                            color = if (synth.epistemicUncertainty > 0.4f) Color(0xFFEF4444) else OmnisEmerald
                        )
                        MetricItem(
                            label = "Resilience",
                            value = "${(synth.systemicResilienceScore * 100).toInt()}%",
                            color = OmnisEmerald
                        )
                    }
                }
            }

            // Divergent perspectives
            item {
                Text(
                    text = "DIVERGENTNÍ POZICE AGENTŮ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = OmnisCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            items(synth.perspectives) { perspective ->
                PerspectiveCard(perspective)
            }

            // Synthesized Resolution
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(20.dp))
                            Text(
                                text = "SYNTETIZOVANÝ KONSENZUS",
                                color = OmnisEmerald,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = synth.synthesizedResolution,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "DOPORUČENÉ INVARIANCE & KROKY:",
                            color = OmnisTextLight,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        synth.actionableGuidelines.forEach { step ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("•", color = OmnisCyan)
                                Text(
                                    text = step,
                                    color = OmnisTextLight,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onSendToChat("Aplikuj dialektickou syntézu na systém: ${synth.synthesizedResolution}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OmnisEmerald, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PŘENÉST VÝSLEDEK DO CHATU", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = color)
        Text(text = label, fontSize = 10.sp, color = OmnisTextLight, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PerspectiveCard(perspective: DialecticPerspective) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = perspective.agentName,
                    fontWeight = FontWeight.Bold,
                    color = OmnisCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = "Divergence: ${(perspective.divergenceScore * 100).toInt()}%",
                    color = OmnisTextLight,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = perspective.roleTitle,
                color = OmnisTextLight,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = perspective.viewpoint,
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            perspective.keyCounterpoints.forEach { pt ->
                Text(
                    text = "▸ $pt",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
