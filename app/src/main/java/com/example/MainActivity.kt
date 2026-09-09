package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OmnisRecord
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: OmnisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OmnisMainScreen(viewModel = viewModel)
            }
        }
    }
}

/**
 * Kept for unit & screenshot test compatibility.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        color = OmnisCyan,
        modifier = modifier.testTag("greeting_text")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmnisMainScreen(viewModel: OmnisViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputQuery by viewModel.inputQuery.collectAsStateWithLifecycle()
    val selectedDomain by viewModel.selectedDomain.collectAsStateWithLifecycle()

    val simEconomic by viewModel.simEconomic.collectAsStateWithLifecycle()
    val simEcoSocial by viewModel.simEcoSocial.collectAsStateWithLifecycle()
    val simTech by viewModel.simTech.collectAsStateWithLifecycle()
    val simPsych by viewModel.simPsych.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark),
        containerColor = OmnisBgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ω", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "O.M.N.I.S.",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = OmnisCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "v2.5",
                                        color = OmnisCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Cognitive Impact Matrix Engine",
                                color = OmnisTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearAllHistory() },
                        modifier = Modifier
                            .testTag("clear_history_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Vymazat relaci",
                            tint = OmnisTextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OmnisPanelDark.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = OmnisPanelDark,
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = OmnisBorderDark,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
            ) {
                NavigationBarItem(
                    selected = activeTab == OmnisTab.CHAT,
                    onClick = { viewModel.setTab(OmnisTab.CHAT) },
                    icon = { Icon(Icons.Default.Send, contentDescription = "Kognitivní Chat") },
                    label = { Text("Chat", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        indicatorColor = OmnisCyan.copy(alpha = 0.2f),
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_chat")
                )
                NavigationBarItem(
                    selected = activeTab == OmnisTab.MATRIX,
                    onClick = { viewModel.setTab(OmnisTab.MATRIX) },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Matice Dopadů") },
                    label = { Text("Matice (4D)", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        indicatorColor = OmnisCyan.copy(alpha = 0.2f),
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_matrix")
                )
                NavigationBarItem(
                    selected = activeTab == OmnisTab.MEMORY,
                    onClick = { viewModel.setTab(OmnisTab.MEMORY) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Paměť") },
                    label = { Text("Paměť", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        indicatorColor = OmnisCyan.copy(alpha = 0.2f),
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_memory")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(OmnisBgDark)
        ) {
            when (activeTab) {
                OmnisTab.CHAT -> ChatView(
                    records = records,
                    isLoading = isLoading,
                    inputQuery = inputQuery,
                    onQueryChange = viewModel::onQueryChange,
                    onSend = { viewModel.sendQuery() },
                    onQuickQuery = { viewModel.sendQuery(it) }
                )
                OmnisTab.MATRIX -> MatrixView(
                    latestRecord = records.lastOrNull { it.role == "assistant" },
                    simEconomic = simEconomic,
                    simEcoSocial = simEcoSocial,
                    simTech = simTech,
                    simPsych = simPsych,
                    onSimChange = { eco, ecoSoc, tech, psych ->
                        viewModel.setSimEconomic(eco)
                        viewModel.setSimEcoSocial(ecoSoc)
                        viewModel.setSimTech(tech)
                        viewModel.setSimPsych(psych)
                    }
                )
                OmnisTab.MEMORY -> MemoryView(records = records)
            }
        }
    }
}

@Composable
fun ChatView(
    records: List<OmnisRecord>,
    isLoading: Boolean,
    inputQuery: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickQuery: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(records.size, isLoading) {
        if (records.isNotEmpty()) {
            listState.animateScrollToItem(records.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(records, key = { it.id }) { record ->
                ChatMessageItem(record = record, onQuickQuery = onQuickQuery)
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = OmnisCyan,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Probíhá kognitivní syntéza & výpočet tenzorů...",
                            color = OmnisCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Input Bar
        Surface(
            color = OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            "Zadejte dotaz pro O.M.N.I.S...",
                            color = OmnisTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedContainerColor = OmnisBgDark,
                        unfocusedContainerColor = OmnisBgDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                IconButton(
                    onClick = onSend,
                    enabled = inputQuery.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputQuery.isNotBlank() && !isLoading) OmnisCyan else OmnisBorderDark
                        )
                        .testTag("send_query_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Odeslat dotaz",
                        tint = if (inputQuery.isNotBlank() && !isLoading) Color.Black else OmnisTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(record: OmnisRecord, onQuickQuery: (String) -> Unit) {
    val isUser = record.role == "user"
    var thoughtsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            color = if (isUser) OmnisViolet.copy(alpha = 0.22f) else OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) OmnisViolet.copy(alpha = 0.5f) else OmnisBorderDark
            ),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .testTag("message_card_${record.id}")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Sender label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "OPERÁTOR" else "O.M.N.I.S. CORE",
                        color = if (isUser) OmnisViolet else OmnisCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!isUser && record.compositeScore > 0f) {
                        Text(
                            text = "Index: ${(record.compositeScore * 100).toInt()}%",
                            color = OmnisEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Introspection thought accordion
                if (!isUser && record.cognitiveProcess.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OmnisBgDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { thoughtsExpanded = !thoughtsExpanded }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kognitivní introspekce",
                                    color = OmnisCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = if (thoughtsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Rozbalit myšlenky",
                                    tint = OmnisCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            AnimatedVisibility(visible = thoughtsExpanded) {
                                Text(
                                    text = record.cognitiveProcess,
                                    color = OmnisTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Main Content
                Text(
                    text = record.content,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                // 4-Dimension Metric Badges
                if (!isUser && record.compositeScore > 0f) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(label = "Ekon", value = record.economicViability, color = OmnisEmerald)
                        MetricPill(label = "Ekol", value = record.ecoSocialRegeneration, color = Color(0xFF2DD4BF))
                        MetricPill(label = "Tech", value = record.technologicalElegance, color = OmnisCyan)
                        MetricPill(label = "Psych", value = record.psychologicalAcceptability, color = OmnisViolet)
                    }
                }

                // Follow-up question chips
                if (!isUser && record.followUpQuestions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val questions = record.followUpQuestions.split("|").filter { it.isNotBlank() }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        questions.forEach { q ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OmnisBgDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQuickQuery(q) }
                            ) {
                                Text(
                                    text = "→ $q",
                                    color = OmnisCyan,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: Float, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = OmnisBgDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = "${(value * 100).toInt()}%",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MatrixView(
    latestRecord: OmnisRecord?,
    simEconomic: Float,
    simEcoSocial: Float,
    simTech: Float,
    simPsych: Float,
    onSimChange: (Float, Float, Float, Float) -> Unit
) {
    val composite = (simEconomic + simEcoSocial + simTech + simPsych) / 4f
    val animatedComposite by animateFloatAsState(targetValue = composite, label = "composite")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Composite Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OmnisPanelDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("matrix_hero_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ČTYŘDIMENZIONÁLNÍ MATICE DOPADŮ",
                        color = OmnisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${(animatedComposite * 100).toInt()} %",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "INTEGRÁLNÍ INDEX HARMONIE",
                        color = OmnisEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Systémová rovnováha mezi ekonomickou životaschopností, ekologickou regenerací, technologickou elegancí a lidskou akceptací.",
                        color = OmnisTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 4 Dimension Interactive Sliders
        item {
            Text(
                text = "INTERAKTIVNÍ CO-KDYŽ SIMULACE",
                color = OmnisTextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            DimensionSliderCard(
                title = "Ekonomická Životaschopnost",
                desc = "Efektivita nákladů a návratnost investice",
                value = simEconomic,
                color = OmnisEmerald,
                testTag = "slider_economic",
                onValueChange = { onSimChange(it, simEcoSocial, simTech, simPsych) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Ekologicko-Sociální Regenerace",
                desc = "Udržitelnost a regenerativní potenciál biosféry",
                value = simEcoSocial,
                color = Color(0xFF2DD4BF),
                testTag = "slider_ecosocial",
                onValueChange = { onSimChange(simEconomic, it, simTech, simPsych) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Technologická Elegance",
                desc = "Modularita, robustnost a čistota kódu",
                value = simTech,
                color = OmnisCyan,
                testTag = "slider_tech",
                onValueChange = { onSimChange(simEconomic, simEcoSocial, it, simPsych) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Psychologická Přijatelnost",
                desc = "Etika, transparentnost a důvěra operátora",
                value = simPsych,
                color = OmnisViolet,
                testTag = "slider_psych",
                onValueChange = { onSimChange(simEconomic, simEcoSocial, simTech, it) }
            )
        }
    }
}

@Composable
fun DimensionSliderCard(
    title: String,
    desc: String,
    value: Float,
    color: Color,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(value * 100).toInt()}%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = desc,
                color = OmnisTextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = OmnisBorderDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MemoryView(records: List<OmnisRecord>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = OmnisPanelDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUTOPOIETICKÁ SÉMANTICKÁ PAMĚŤ",
                        color = OmnisCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Lokální Room perzistence propojená s ontologickým rámcem a tenzory Matice dopadů.",
                        color = OmnisTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "ZAZNAMENANÉ KOGNITIVNÍ OTISKY (${records.size})",
                color = OmnisTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        items(records, key = { it.id }) { record ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OmnisPanelDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (record.role == "user") "OTISK [DOTAZ]" else "OTISK [SYNTÉZA]",
                            color = if (record.role == "user") OmnisViolet else OmnisCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "768-dim hash #${record.id}",
                            color = OmnisTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = record.content.take(160) + if (record.content.length > 160) "..." else "",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
