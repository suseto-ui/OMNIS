package com.example.ui.cockpit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisArtifact
import com.example.data.OmnisGoal
import com.example.data.OmnisRecord
import com.example.ui.theme.*

@Composable
fun OmnisCockpitView(
    latestRecord: OmnisRecord?,
    activeGoals: List<OmnisGoal>,
    recentArtifacts: List<OmnisArtifact>,
    onTabSwitch: (com.example.ui.OmnisTab) -> Unit
) {
    val scrollState = rememberScrollState()
    val healthScore = latestRecord?.compositeScore ?: 0.75f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .navigationBarsPadding()
            .testTag("cockpit_root")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "O.M.N.I.S. COCKPIT",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "AUTONOMOUS MISSION CONTROL",
                    color = OmnisCyan,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // System Health Indicator
            SystemHealthGauge(healthScore)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grid of Quick Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "MATRIX STATE",
                value = if (latestRecord != null) "STABILIZED" else "UNKNOWN",
                icon = Icons.Default.Grid4x4,
                color = OmnisCyan,
                onClick = { onTabSwitch(com.example.ui.OmnisTab.MATRIX) }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "NEXUS NODES",
                value = "5 ACTIVE",
                icon = Icons.Default.Hub,
                color = OmnisViolet,
                onClick = { onTabSwitch(com.example.ui.OmnisTab.NEXUS) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active Goals Section
        SectionHeader("ACTIVE MISSIONS", Icons.Default.Flag) { onTabSwitch(com.example.ui.OmnisTab.GOALS) }
        Spacer(modifier = Modifier.height(8.dp))
        if (activeGoals.isEmpty()) {
            EmptyStateBox("Žádné aktivní cíle")
        } else {
            activeGoals.take(3).forEach { goal ->
                GoalSummaryItem(goal)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Artifacts
        SectionHeader("KNOWLEDGE ARTIFACTS", Icons.Default.Inventory2) { onTabSwitch(com.example.ui.OmnisTab.ARTIFACTS) }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recentArtifacts.take(5)) { artifact ->
                ArtifactMiniCard(artifact)
            }
            if (recentArtifacts.isEmpty()) {
                item { EmptyStateBox("Žádné artefakty", modifier = Modifier.width(200.dp)) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Telemetry Preview
        SectionHeader("TELEMETRY STREAM", Icons.Default.Dns) { onTabSwitch(com.example.ui.OmnisTab.TELEMETRY) }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = OmnisCardDark,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("HEARTBEAT", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("NOMINAL", color = OmnisEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(OmnisBorderDark))
                Column {
                    Text("LATENCY", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("42 ms", color = OmnisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(OmnisBorderDark))
                Column {
                    Text("THROUGHPUT", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("0.8k/min", color = OmnisViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Nav bar padding
    }
}

@Composable
fun SystemHealthGauge(score: Float) {
    val color = when {
        score > 0.8f -> OmnisEmerald
        score > 0.5f -> OmnisCyan
        else -> Color.Red
    }
    
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { score },
            modifier = Modifier.size(56.dp),
            color = color,
            strokeWidth = 4.dp,
            trackColor = OmnisBorderDark
        )
        Text(
            "${(score * 100).toInt()}%",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 98.dp)
            .clickable(onClick = onClick),
        color = OmnisCardDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = OmnisTextMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, onMore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(16.dp))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Text(
            "DETAIL",
            color = OmnisCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onMore() }
        )
    }
}

@Composable
fun GoalSummaryItem(goal: OmnisGoal) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = OmnisPanelDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(OmnisCyan.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${(goal.progress * 100).toInt()}%", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(goal.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(goal.status, color = OmnisTextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ArtifactMiniCard(artifact: OmnisArtifact) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        color = OmnisPanelDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(artifact.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).background(OmnisCyan, CircleShape))
                Text(artifact.language.uppercase(), color = OmnisCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyStateBox(text: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Box(
        modifier = modifier
            .height(60.dp)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .border(1.dp, OmnisBorderDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = OmnisTextMuted, fontSize = 11.sp)
    }
}
