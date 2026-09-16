package com.example

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OmnisRecord
import com.example.TestSemanticChatView
import androidx.compose.material.icons.filled.Science
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.OctagonDashboard
import com.example.ui.theme.*
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val viewModel: OmnisViewModel by viewModels()
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OmnisMainScreen(
                    viewModel = viewModel,
                    onSpeak = { text ->
                        if (text.length >= TextToSpeech.getMaxSpeechInputLength()) {
                            val chunks = text.chunked(TextToSpeech.getMaxSpeechInputLength() - 1)
                            chunks.forEachIndexed { index, chunk ->
                                tts?.speak(chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, null)
                            }
                        } else {
                            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    },
                    onExportPdf = { record -> exportToPdf(record) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("cs", "CZ"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Český jazyk pro TTS není dostupný.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Inicializace TTS selhala.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun exportToPdf(record: OmnisRecord) {
        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.BLUE
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 50f

        canvas.drawText("O.M.N.I.S. Kognitivní Report", 50f, y, headerPaint)
        y += 40f

        textPaint.isFakeBoldText = true
        canvas.drawText("Identifikátor: #${record.id}", 50f, y, textPaint)
        y += 20f
        canvas.drawText("Role: ${record.role.uppercase()}", 50f, y, textPaint)
        y += 20f
        canvas.drawText("Čas: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp))}", 50f, y, textPaint)
        y += 40f

        textPaint.isFakeBoldText = false
        val contentLines = record.content.split("\n")
        val maxWidth = 500f

        for (line in contentLines) {
            val words = line.split(" ")
            var currentLine = StringBuilder()
            
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                val width = textPaint.measureText(testLine)
                
                if (width > maxWidth) {
                    canvas.drawText(currentLine.toString(), 50f, y, textPaint)
                    y += 20f
                    currentLine = StringBuilder(word)
                    
                    if (y > 780) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 50f
                    }
                } else {
                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
                }
            }
            
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine.toString(), 50f, y, textPaint)
                y += 20f
            }

            if (y > 780) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
        }

        if (record.cognitiveProcess.isNotBlank()) {
            y += 20f
            textPaint.isFakeBoldText = true
            canvas.drawText("Kognitivní Introspekce:", 50f, y, textPaint)
            y += 20f
            textPaint.isFakeBoldText = false
            
            val processLines = record.cognitiveProcess.split("\n")
            for (line in processLines) {
                canvas.drawText(line.take(80), 50f, y, textPaint)
                y += 18f
                if (y > 780) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
            }
        }

        pdfDocument.finishPage(page)

        try {
            val file = java.io.File(getExternalFilesDir(null), "omnis_export_${record.id}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF uloženo: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            
            // Share file
            val authority = "${packageName}.provider"
            val uri = androidx.core.content.FileProvider.getUriForFile(this, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Sdílet PDF"))
        } catch (e: Exception) {
            Toast.makeText(this, "Export selhal: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OmnisMainScreen(
    viewModel: OmnisViewModel,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputQuery by viewModel.inputQuery.collectAsStateWithLifecycle()
    val ocrValidationState by viewModel.ocrValidationState.collectAsStateWithLifecycle()
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDevLockDialog by remember { mutableStateOf(false) }
    var devPassword by remember { mutableStateOf("") }
    var devUnlocked by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ocrValidationState?.let { state ->
        com.example.ui.OcrValidationDialog(
            state = state,
            onTextChanged = viewModel::updateOcrValidationText,
            onConfirm = viewModel::confirmOcrValidation,
            onCancel = viewModel::cancelOcrValidation
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Potvrdit smazání") },
            text = { Text("Opravdu chcete vymazat celou historii paměti O.M.N.I.S.? Tato akce je nevratná.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Smazat", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Zrušit") }
            }
        )
    }

    if (showDevLockDialog) {
        AlertDialog(
            onDismissRequest = { showDevLockDialog = false },
            title = { Text("Kognitivní autorizace") },
            text = {
                Column {
                    Text("Zadejte přístupové heslo pro DEV rozhraní:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = devPassword,
                        onValueChange = { devPassword = it },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (devPassword == "omnis2026") {
                            devUnlocked = true
                            viewModel.setTab(OmnisTab.DEV)
                        }
                        showDevLockDialog = false
                        devPassword = ""
                    }
                ) { Text("Ověřit") }
            },
            dismissButton = {
                TextButton(onClick = { showDevLockDialog = false }) { Text("Zrušit") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = OmnisBgDark,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ω", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Column {
                        Text("O.M.N.I.S.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Kognitivní Řízení v2.7", color = OmnisCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 12.dp))
                
                NavigationDrawerItem(
                    label = { Text("Kognitivní Chat", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    selected = activeTab == OmnisTab.CHAT,
                    onClick = {
                        viewModel.setTab(OmnisTab.CHAT)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(52.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Analytický Přehled", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    selected = activeTab == OmnisTab.ANALYTICS,
                    onClick = {
                        viewModel.setTab(OmnisTab.ANALYTICS)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(52.dp)
                )
                
                NavigationDrawerItem(
                    label = { Text("Historická Paměť & Archiv", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    selected = activeTab == OmnisTab.MEMORY,
                    onClick = {
                        viewModel.setTab(OmnisTab.MEMORY)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(52.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Kognitivní Uzly", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    selected = activeTab == OmnisTab.NODES,
                    onClick = {
                        viewModel.setTab(OmnisTab.NODES)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(52.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Správa Témat & Šablony", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    selected = activeTab == OmnisTab.DASHBOARD,
                    onClick = {
                        viewModel.setTab(OmnisTab.DASHBOARD)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(52.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Octagon 8D Matice", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    selected = activeTab == OmnisTab.MATRIX,
                    onClick = {
                        viewModel.setTab(OmnisTab.MATRIX)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(52.dp)
                )

                if (devUnlocked) {
                    NavigationDrawerItem(
                        label = { Text("Sémantický Test", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selected = activeTab == OmnisTab.TEST_SEMANTIC,
                        onClick = {
                            viewModel.setTab(OmnisTab.TEST_SEMANTIC)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Science, contentDescription = null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = OmnisAmber.copy(alpha = 0.15f),
                            selectedIconColor = OmnisAmber,
                            selectedTextColor = OmnisAmber,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = OmnisTextMuted,
                            unselectedTextColor = OmnisTextMuted
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(52.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Vývojová Laboratoř", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selected = activeTab == OmnisTab.DEV,
                        onClick = {
                            viewModel.setTab(OmnisTab.DEV)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = OmnisAmber.copy(alpha = 0.15f),
                            selectedIconColor = OmnisAmber,
                            selectedTextColor = OmnisAmber,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = OmnisTextMuted,
                            unselectedTextColor = OmnisTextMuted
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(52.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(OmnisBgDark),
            containerColor = OmnisBgDark,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier
                                .testTag("hamburger_menu_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Otevřít menu",
                                tint = OmnisCyan
                            )
                        }
                    },
                    title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.combinedClickable(
                            onClick = { },
                            onLongClick = { 
                                if (!devUnlocked) showDevLockDialog = true 
                                else viewModel.setTab(OmnisTab.DEV)
                            }
                        )
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
                                    fontSize = 16.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = OmnisCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "v2.7",
                                        color = OmnisCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .height(48.dp),
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .testTag("clear_history_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Vymazat relaci",
                            tint = OmnisTextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OmnisPanelDark.copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .background(OmnisBgDark)
        ) {
            when (activeTab) {
                OmnisTab.CHAT -> {
                    val selectedDomains by viewModel.selectedDomains.collectAsStateWithLifecycle()
                    val focusedDomain by viewModel.focusedDomain.collectAsStateWithLifecycle()
                    val selectedRecord by viewModel.selectedRecordForDetail.collectAsStateWithLifecycle()
                    val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()

                    Box(modifier = Modifier.fillMaxSize()) {
                        ChatView(
                            records = records,
                            isLoading = isLoading,
                            isOcrLoading = isOcrLoading,
                            inputQuery = inputQuery,
                            onQueryChange = viewModel::onQueryChange,
                            onSend = { viewModel.sendQuery() },
                            onQuickQuery = { viewModel.sendQuery(it) },
                            onSpeak = onSpeak,
                            onExportPdf = onExportPdf,
                            onImageSelected = { uri -> viewModel.extractTextFromImage(uri) },
                            onDocumentSelected = { uri, name, mimeType -> viewModel.extractTextFromDocument(uri, name, mimeType) },
                            scrollToId = viewModel.scrollToId.collectAsStateWithLifecycle().value,
                            onScrollComplete = { viewModel.clearScrollJump() },
                            selectedDomains = selectedDomains,
                            onDomainClick = viewModel::toggleDomainSelection,
                            onDomainLongClick = viewModel::focusDomain,
                            onAuthorizeRecord = viewModel::authorizeBlockedRecord
                        )

                        // Multi-Domain Synthesis Panel
                        if (selectedDomains.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = OmnisPanelDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                                tonalElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "SYNTÉZNÍ PANEL (${selectedDomains.size})",
                                            color = OmnisCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = selectedDomains.joinToString(", "),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { viewModel.clearSelection() }) {
                                            Text("Zrušit", color = OmnisTextMuted, fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.runMultiDomainSynthesis("COMPARE") },
                                            colors = ButtonDefaults.buttonColors(containerColor = OmnisBgDark),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Text("Srovnat", color = Color.White, fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.runMultiDomainSynthesis("HARMONIZE") },
                                            colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Text("Harmonizovat", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Domain Detail Modal / Sheet
                        if (focusedDomain != null && selectedRecord != null) {
                            ModalBottomSheet(
                                onDismissRequest = { viewModel.clearFocus() },
                                containerColor = OmnisBgDark,
                                dragHandle = { BottomSheetDefaults.DragHandle(color = OmnisBorderDark) }
                            ) {
                                DomainDetailContent(
                                    domain = focusedDomain!!,
                                    record = selectedRecord!!,
                                    onOptimize = { viewModel.optimizeForDomain(focusedDomain!!, selectedRecord!!) }
                                )
                            }
                        }
                    }
                }
                OmnisTab.MATRIX -> {
                    val fixedDomains by viewModel.fixedDomains.collectAsStateWithLifecycle()
                    val isComparing by viewModel.isComparing.collectAsStateWithLifecycle()
                    val comparisonResult by viewModel.comparisonResult.collectAsStateWithLifecycle()

                    OctagonDashboard(
                        records = records,
                        latestRecord = records.lastOrNull { it.role == "assistant" },
                        simSys = viewModel.simSys.collectAsStateWithLifecycle().value,
                        simEcon = viewModel.simEcon.collectAsStateWithLifecycle().value,
                        simPsych = viewModel.simPsych.collectAsStateWithLifecycle().value,
                        simEco = viewModel.simEco.collectAsStateWithLifecycle().value,
                        simLaw = viewModel.simLaw.collectAsStateWithLifecycle().value,
                        simSec = viewModel.simSec.collectAsStateWithLifecycle().value,
                        simPhys = viewModel.simPhys.collectAsStateWithLifecycle().value,
                        simSoc = viewModel.simSoc.collectAsStateWithLifecycle().value,
                        fixedDomains = fixedDomains,
                        onToggleFix = viewModel::toggleDomainFixation,
                        onSimChange = { sys, econ, psych, eco, law, sec, phys, soc ->
                            viewModel.setSimSys(sys)
                            viewModel.setSimEcon(econ)
                            viewModel.setSimPsych(psych)
                            viewModel.setSimEco(eco)
                            viewModel.setSimLaw(law)
                            viewModel.setSimSec(sec)
                            viewModel.setSimPhys(phys)
                            viewModel.setSimSoc(soc)
                        },
                        isComparing = isComparing,
                        comparisonResult = comparisonResult,
                        onSynthesize = { selectedIds ->
                            viewModel.synthesizeSelectedRecords(selectedIds)
                        },
                        onClearComparison = {
                            viewModel.clearComparison()
                        }
                    )
                }
                OmnisTab.MEMORY -> MemoryView(
                    records = records,
                    onItemClick = { record ->
                        viewModel.jumpToContext(record)
                    }
                )
                OmnisTab.ANALYTICS -> OctagonDashboard(
                    records = records,
                    latestRecord = records.lastOrNull { it.role == "assistant" },
                    simSys = viewModel.simSys.collectAsStateWithLifecycle().value,
                    simEcon = viewModel.simEcon.collectAsStateWithLifecycle().value,
                    simPsych = viewModel.simPsych.collectAsStateWithLifecycle().value,
                    simEco = viewModel.simEco.collectAsStateWithLifecycle().value,
                    simLaw = viewModel.simLaw.collectAsStateWithLifecycle().value,
                    simSec = viewModel.simSec.collectAsStateWithLifecycle().value,
                    simPhys = viewModel.simPhys.collectAsStateWithLifecycle().value,
                    simSoc = viewModel.simSoc.collectAsStateWithLifecycle().value,
                    fixedDomains = viewModel.fixedDomains.collectAsStateWithLifecycle().value,
                    onToggleFix = viewModel::toggleDomainFixation,
                    onSimChange = { sys, econ, psych, eco, law, sec, phys, soc ->
                        viewModel.setSimSys(sys)
                        viewModel.setSimEcon(econ)
                        viewModel.setSimPsych(psych)
                        viewModel.setSimEco(eco)
                        viewModel.setSimLaw(law)
                        viewModel.setSimSec(sec)
                        viewModel.setSimPhys(phys)
                        viewModel.setSimSoc(soc)
                    },
                    isComparing = viewModel.isComparing.collectAsStateWithLifecycle().value,
                    comparisonResult = viewModel.comparisonResult.collectAsStateWithLifecycle().value,
                    onSynthesize = { selectedIds -> viewModel.synthesizeSelectedRecords(selectedIds) },
                    onClearComparison = { viewModel.clearComparison() }
                )
                OmnisTab.NODES -> MemoryView(
                    records = records,
                    onItemClick = { record -> viewModel.jumpToContext(record) }
                )
                OmnisTab.DASHBOARD -> MemoryView(
                    records = records,
                    onItemClick = { record -> viewModel.jumpToContext(record) }
                )
                OmnisTab.DEV -> DevView()
                OmnisTab.TEST_SEMANTIC -> {
                    val testRecords by viewModel.testSemanticRecords.collectAsStateWithLifecycle()
                    val testQuery by viewModel.inputQuery.collectAsStateWithLifecycle()
                    TestSemanticChatView(
                        records = testRecords,
                        isLoading = isLoading,
                        inputQuery = testQuery,
                        onQueryChange = viewModel::onQueryChange,
                        onSend = { viewModel.sendTestSemanticQuery(testQuery) },
                        onWeightChange = viewModel::updateSemanticAnchorWeight,
                        onReSynthesize = viewModel::reSynthesizeTestRecord
                    )
                }
            }
        }
    }
}
}

@Composable
fun ChatView(
    records: List<OmnisRecord>,
    isLoading: Boolean,
    isOcrLoading: Boolean = false,
    inputQuery: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickQuery: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit,
    onImageSelected: (Uri) -> Unit = {},
    onDocumentSelected: (Uri, String, String) -> Unit = { _, _, _ -> },
    scrollToId: Long? = null,
    onScrollComplete: () -> Unit = {},
    selectedDomains: Set<String> = emptySet(),
    onDomainClick: (String) -> Unit = {},
    onDomainLongClick: (String, OmnisRecord) -> Unit = { _, _ -> },
    onAuthorizeRecord: (OmnisRecord) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Handle scroll jump
    LaunchedEffect(scrollToId) {
        scrollToId?.let { id ->
            val index = records.indexOfFirst { it.id == id }
            if (index != -1) {
                listState.animateScrollToItem(index)
                onScrollComplete()
            }
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImageSelected(it) }
    }

    // STT Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let { onQueryChange(inputQuery + " " + it) }
        }
    }

    // File Picker Launcher
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                val fileName = contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "Soubor"

                val extension = fileName.substringAfterLast('.', "").lowercase()
                
                if (extension == "pdf") {
                    onDocumentSelected(it, fileName, "application/pdf")
                } else {
                    contentResolver.openInputStream(it)?.use { stream ->
                        val contentText = if (extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx")) {
                            "[Binární dokument Office: $fileName - Obsah není přímo čitelný jako text]"
                        } else {
                            try {
                                stream.bufferedReader(Charsets.UTF_8).readText()
                            } catch (e: Exception) {
                                "[Soubor: $fileName - Obsah nelze interpretovat jako text]"
                            }
                        }
                        onQueryChange(inputQuery + "\n\n--- Obsah souboru ($fileName) ---\n" + contentText + "\n--- Konec souboru ---")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Nepodařilo se načíst soubor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(records.size, isLoading) {
        if (records.isNotEmpty() && scrollToId == null) {
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
                ChatMessageItem(
                    record = record, 
                    onQuickQuery = onQuickQuery,
                    onSpeak = onSpeak,
                    onExportPdf = onExportPdf,
                    selectedDomains = selectedDomains,
                    onDomainClick = onDomainClick,
                    onDomainLongClick = onDomainLongClick,
                    onAuthorize = { onAuthorizeRecord(record) }
                )
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
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        fileLauncher.launch(arrayOf(
                            "text/*", 
                            "application/pdf", 
                            "application/msword", 
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/json",
                            "application/xml",
                            "application/javascript",
                            "application/x-python",
                            "application/x-sh",
                            "application/x-php",
                            "text/markdown",
                            "text/x-python",
                            "text/x-java-source",
                            "text/html",
                            "text/css"
                        )) 
                    }, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Přiložit soubor", tint = OmnisCyan, modifier = Modifier.size(20.dp))
                }

                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Vyfotit/Obrázek", tint = OmnisCyan, modifier = Modifier.size(20.dp))
                }

                Box(modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) {
                    OutlinedTextField(
                        value = inputQuery,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                "Zpráva pro O.M.N.I.S...",
                                color = OmnisTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "cs-CZ")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Diktujte dotaz...")
                                    }
                                    speechLauncher.launch(intent)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Diktovat", tint = OmnisCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                    
                    if (isOcrLoading) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(OmnisBgDark.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .border(1.dp, OmnisCyan, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(color = OmnisCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("OCR...", color = OmnisCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onSend,
                    enabled = inputQuery.isNotBlank() && !isLoading && !isOcrLoading,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputQuery.isNotBlank() && !isLoading && !isOcrLoading) OmnisCyan else OmnisBorderDark
                        )
                        .testTag("send_query_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Odeslat dotaz",
                        tint = if (inputQuery.isNotBlank() && !isLoading && !isOcrLoading) Color.Black else OmnisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    record: OmnisRecord, 
    onQuickQuery: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit,
    selectedDomains: Set<String> = emptySet(),
    onDomainClick: (String) -> Unit = {},
    onDomainLongClick: (String, OmnisRecord) -> Unit = { _, _ -> },
    onAuthorize: () -> Unit = {}
) {
    val isUser = record.role == "user"
    var thoughtsExpanded by remember { mutableStateOf(false) }

    val isBlocked = !isUser && record.defenseTier == "BLOCKED"
    val isWarning = !isUser && record.defenseTier == "WARNING"
    val isApproved = !isUser && record.defenseTier == "APPROVED"

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
            color = when {
                isUser -> OmnisViolet.copy(alpha = 0.22f)
                isBlocked -> Color(0xFF2E1111)
                isWarning -> Color(0xFF261A08)
                else -> OmnisPanelDark
            },
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when {
                    isUser -> OmnisViolet.copy(alpha = 0.5f)
                    isBlocked -> Color(0xFFEF4444)
                    isWarning -> Color(0xFFF59E0B)
                    else -> OmnisBorderDark
                }
            ),
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.88f else 0.96f)
                .testTag("message_card_${record.id}")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Sender label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (isUser) Icons.Default.Person else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isUser) OmnisViolet else OmnisCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isUser) "OPERÁTOR" else "O.M.N.I.S. CORE",
                            color = if (isUser) OmnisViolet else OmnisCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (!isUser && record.compositeScore > 0f) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onSpeak(record.content) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Přehrát", tint = OmnisCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onExportPdf(record) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Download, contentDescription = "Exportovat PDF", tint = OmnisCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Multi-Layer Defense Status Banner
                if (!isUser) {
                    when {
                        isBlocked -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("defense_blocked_banner")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "VÝSTUP ZABLOKOVÁN: HUMAN-IN-THE-LOOP",
                                            color = Color(0xFFEF4444),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (record.defenseNotes.isNotBlank()) {
                                        Text(
                                            text = record.defenseNotes,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                                        )
                                    }
                                    Button(
                                        onClick = onAuthorize,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("btn_authorize_human_loop")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Autorizovat operátorem", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        isWarning -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("defense_warning_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                    Column {
                                        Text(
                                            text = "Zvýšená pozornost operátora (Spolehlivost: ${(record.compositeScore * 100).toInt()}%)",
                                            color = Color(0xFFF59E0B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (record.defenseNotes.isNotBlank()) {
                                            Text(
                                                text = record.defenseNotes,
                                                color = Color(0xFFFCD34D),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        isApproved && record.compositeScore > 0f -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OmnisCyan.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .testTag("defense_verified_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "MULTI-LAYER VERIFIED (${(record.compositeScore * 100).toInt()}%)",
                                        color = OmnisCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                record.attachedImagePath?.let { path ->
                    val file = java.io.File(path)
                    if (file.exists()) {
                        coil.compose.AsyncImage(
                            model = file,
                            contentDescription = "Připojený obrázek",
                            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

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

                // 8-Dimension Metric Badges
                if (!isUser && record.compositeScore > 0f) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricPill("Sys", record.valSys, Color(0xFF60A5FA), Icons.Default.Settings, selectedDomains.contains("Sys"), onClick = { onDomainClick("Sys") }, onLongClick = { onDomainLongClick("Sys", record) })
                            MetricPill("Econ", record.valEcon, Color(0xFFFBBF24), Icons.Default.Paid, selectedDomains.contains("Econ"), onClick = { onDomainClick("Econ") }, onLongClick = { onDomainLongClick("Econ", record) })
                            MetricPill("Psych", record.valPsych, Color(0xFFC084FC), Icons.Default.Face, selectedDomains.contains("Psych"), onClick = { onDomainClick("Psych") }, onLongClick = { onDomainLongClick("Psych", record) })
                            MetricPill("Eco", record.valEco, Color(0xFF34D399), Icons.Default.Spa, selectedDomains.contains("Eco"), onClick = { onDomainClick("Eco") }, onLongClick = { onDomainLongClick("Eco", record) })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricPill("Law", record.valLaw, Color(0xFFFB7185), Icons.Default.Gavel, selectedDomains.contains("Law"), onClick = { onDomainClick("Law") }, onLongClick = { onDomainLongClick("Law", record) })
                            MetricPill("Sec", record.valSec, Color(0xFFEF4444), Icons.Default.Security, selectedDomains.contains("Sec"), onClick = { onDomainClick("Sec") }, onLongClick = { onDomainLongClick("Sec", record) })
                            MetricPill("Phys", record.valPhys, Color(0xFFFB923C), Icons.Default.Speed, selectedDomains.contains("Phys"), onClick = { onDomainClick("Phys") }, onLongClick = { onDomainLongClick("Phys", record) })
                            MetricPill("Soc", record.valSoc, Color(0xFFF472B6), Icons.Default.Groups, selectedDomains.contains("Soc"), onClick = { onDomainClick("Soc") }, onLongClick = { onDomainLongClick("Soc", record) })
                        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetricPill(
    label: String, 
    value: Float, 
    color: Color, 
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color.copy(alpha = 0.25f) else OmnisBgDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else color.copy(alpha = 0.4f)),
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, color = OmnisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${(value * 100).toInt()}%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}



@Composable
fun DomainDetailContent(
    domain: String,
    record: OmnisRecord,
    onOptimize: () -> Unit
) {
    val color = when(domain) {
        "Sys" -> Color(0xFF60A5FA)
        "Econ" -> Color(0xFFFBBF24)
        "Psych" -> Color(0xFFC084FC)
        "Eco" -> Color(0xFF34D399)
        "Law" -> Color(0xFFFB7185)
        "Sec" -> Color(0xFFEF4444)
        "Phys" -> Color(0xFFFB923C)
        "Soc" -> Color(0xFFF472B6)
        else -> OmnisCyan
    }
    
    val value = when(domain) {
        "Sys" -> record.valSys
        "Econ" -> record.valEcon
        "Psych" -> record.valPsych
        "Eco" -> record.valEco
        "Law" -> record.valLaw
        "Sec" -> record.valSec
        "Phys" -> record.valPhys
        "Soc" -> record.valSoc
        else -> 0f
    }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.2f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = domain.uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Column {
                Text(text = "KOGNITIVNÍ DIAGNOSTIKA", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(text = "Úroveň integrity: ${(value * 100).toInt()}%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "ANALÝZA ODOZVY", color = OmnisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OmnisPanelDark,
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
        ) {
            Text(
                text = "Tato odpověď vykazuje v doméně $domain úroveň ${(value * 100).toInt()}%. Systém identifikoval vazby na sémantické uzly v textu, které ovlivňují stabilitu celkového indexu harmonie.",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "RIZIKA & LIMITACE", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "• Potenciální entropie při dlouhodobé fixaci parametrů.\n• Nutnost manuální rekalibrace při změně systémových proměnných.",
            color = OmnisTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOptimize,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "OPTIMALIZOVAT PRO ${domain.uppercase()}", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DimensionSliderCard(
    title: String,
    desc: String,
    value: Float,
    color: Color,
    icon: ImageVector,
    testTag: String,
    isFixed: Boolean = false,
    onToggleFix: () -> Unit = {},
    onValueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFixed) color else OmnisBorderDark),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onToggleFix, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isFixed) Icons.Default.Lock else icon, 
                            contentDescription = null, 
                            tint = color, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
fun DevInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = OmnisTextMuted, fontSize = 12.sp)
        Text(text = value, color = OmnisCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun DevView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisAmber),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DEV ROZHRANÍ (LABORATOŘ)",
                    color = OmnisAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Přístup k diagnostice systému a nízkoúrovňovým parametrům.",
                    color = OmnisTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Systémové informace", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                DevInfoRow("Verze jádra", "2.6.0-stable")
                DevInfoRow("Model", "Gemini 1.5 Pro (via Vertex)")
                DevInfoRow("Latence", "1.2s avg")
                DevInfoRow("Databáze", "Room/SQLite (omnis_local_db)")
            }
        }
        
        Text(
            text = "Diagnostika historie: Databáze se nachází v interním úložišti aplikace: /data/data/com.example/databases/omnis_local_db. Přístup je možný přes App Inspection v IDE nebo exportem zálohy.",
            color = OmnisTextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MemoryView(records: List<OmnisRecord>, onItemClick: (OmnisRecord) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OmnisPanelDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "AUTOPOIETICKÁ SÉMANTICKÁ PAMĚŤ",
                        color = OmnisCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Lokální Room perzistence propojená s ontologickým rámcem.",
                        color = OmnisTextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "ZAZNAMENANÉ KOGNITIVNÍ OTISKY (${records.size})",
                color = OmnisTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        items(records.reversed(), key = { it.id }) { record ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OmnisPanelDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(record) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (record.role == "user") "OTISK [DOTAZ]" else "OTISK [SYNTÉZA]",
                            color = if (record.role == "user") OmnisViolet else OmnisCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "hash #${record.id}",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = record.content.take(120) + if (record.content.length > 120) "..." else "",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    record.attachedImagePath?.let { path ->
                        val file = java.io.File(path)
                        if (file.exists()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Zdrojový obrázek přiložen",
                                    color = OmnisCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
