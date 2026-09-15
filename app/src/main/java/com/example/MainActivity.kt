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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OmnisRecord
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.theme.*
import java.io.FileOutputStream
import java.util.Locale

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
                    onSpeak = { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) },
                    onExportPdf = { record -> exportToPdf(record) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("cs", "CZ")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun exportToPdf(record: OmnisRecord) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("O.M.N.I.S. Export", 50f, 50f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        var y = 80f
        canvas.drawText("Role: ${record.role}", 50f, y, paint)
        y += 20f
        canvas.drawText("Datum: ${java.util.Date(record.timestamp)}", 50f, y, paint)
        y += 30f

        val lines = record.content.split("\n")
        for (line in lines) {
            if (y > 800) break
            canvas.drawText(line.take(80), 50f, y, paint)
            y += 15f
        }

        pdfDocument.finishPage(page)

        try {
            val file = java.io.File(getExternalFilesDir(null), "omnis_export_${record.id}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF uloženo: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            
            // Share file
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.provider", file)
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
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDevLockDialog by remember { mutableStateOf(false) }
    var devPassword by remember { mutableStateOf("") }
    var devUnlocked by remember { mutableStateOf(false) }

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
                                        text = "v2.6",
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
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kognitivní Chat") },
                    label = { Text("Chat", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        indicatorColor = OmnisCyan.copy(alpha = 0.2f),
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == OmnisTab.MATRIX,
                    onClick = { viewModel.setTab(OmnisTab.MATRIX) },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Matice Dopadů") },
                    label = { Text("Matice", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        indicatorColor = OmnisCyan.copy(alpha = 0.2f),
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == OmnisTab.MEMORY,
                    onClick = { viewModel.setTab(OmnisTab.MEMORY) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Paměť") },
                    label = { Text("Paměť", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OmnisCyan,
                        selectedTextColor = OmnisCyan,
                        indicatorColor = OmnisCyan.copy(alpha = 0.2f),
                        unselectedIconColor = OmnisTextMuted,
                        unselectedTextColor = OmnisTextMuted
                    )
                )
                if (devUnlocked) {
                    NavigationBarItem(
                        selected = activeTab == OmnisTab.DEV,
                        onClick = { viewModel.setTab(OmnisTab.DEV) },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Laboratoř") },
                        label = { Text("Laboratoř", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OmnisAmber,
                            selectedTextColor = OmnisAmber,
                            indicatorColor = OmnisAmber.copy(alpha = 0.2f),
                            unselectedIconColor = OmnisTextMuted,
                            unselectedTextColor = OmnisTextMuted
                        )
                    )
                }
            }
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
                OmnisTab.CHAT -> ChatView(
                    records = records,
                    isLoading = isLoading,
                    inputQuery = inputQuery,
                    onQueryChange = viewModel::onQueryChange,
                    onSend = { viewModel.sendQuery() },
                    onQuickQuery = { viewModel.sendQuery(it) },
                    onSpeak = onSpeak,
                    onExportPdf = onExportPdf
                )
                OmnisTab.MATRIX -> MatrixView(
                    latestRecord = records.lastOrNull { it.role == "assistant" },
                    simSys = viewModel.simSys.collectAsStateWithLifecycle().value,
                    simEcon = viewModel.simEcon.collectAsStateWithLifecycle().value,
                    simPsych = viewModel.simPsych.collectAsStateWithLifecycle().value,
                    simEco = viewModel.simEco.collectAsStateWithLifecycle().value,
                    simLaw = viewModel.simLaw.collectAsStateWithLifecycle().value,
                    simSec = viewModel.simSec.collectAsStateWithLifecycle().value,
                    simPhys = viewModel.simPhys.collectAsStateWithLifecycle().value,
                    simSoc = viewModel.simSoc.collectAsStateWithLifecycle().value,
                    onSimChange = { sys, econ, psych, eco, law, sec, phys, soc ->
                        viewModel.setSimSys(sys)
                        viewModel.setSimEcon(econ)
                        viewModel.setSimPsych(psych)
                        viewModel.setSimEco(eco)
                        viewModel.setSimLaw(law)
                        viewModel.setSimSec(sec)
                        viewModel.setSimPhys(phys)
                        viewModel.setSimSoc(soc)
                    }
                )
                OmnisTab.MEMORY -> MemoryView(records = records)
                OmnisTab.DEV -> DevView()
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
    onQuickQuery: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

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

                contentResolver.openInputStream(it)?.use { stream ->
                    val extension = fileName.substringAfterLast('.', "").lowercase()
                    
                    val contentText = if (extension == "pdf") {
                        "[PDF Dokument: $fileName - Obsah PDF není přímo čitelný jako text v této verzi, ale soubor byl zaznamenán]"
                    } else if (extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx")) {
                        "[Binární dokument Office: $fileName - Obsah není přímo čitelný jako text]"
                    } else {
                        // Zkusíme číst jako text s UTF-8, pokud selže, zkusíme jiné kódování
                        try {
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        } catch (e: Exception) {
                            // Fallback pro binární nebo jinak kódované soubory
                            "[Soubor: $fileName - Obsah nelze interpretovat jako text]"
                        }
                    }
                    
                    onQueryChange(inputQuery + "\n\n--- Obsah souboru ($fileName) ---\n" + contentText + "\n--- Konec souboru ---")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Nepodařilo se načíst soubor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                ChatMessageItem(
                    record = record, 
                    onQuickQuery = onQuickQuery,
                    onSpeak = onSpeak,
                    onExportPdf = onExportPdf
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
                IconButton(onClick = { 
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
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Přiložit soubor", tint = OmnisCyan)
                }

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
                    maxLines = 3,
                    trailingIcon = {
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "cs-CZ")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Diktujte dotaz...")
                            }
                            speechLauncher.launch(intent)
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Diktovat", tint = OmnisCyan)
                        }
                    }
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
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Odeslat dotaz",
                        tint = if (inputQuery.isNotBlank() && !isLoading) Color.Black else OmnisTextMuted
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
    onExportPdf: (OmnisRecord) -> Unit
) {
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
                            MetricPill("Sys", record.valSys, Color(0xFF60A5FA), Icons.Default.Settings)
                            MetricPill("Econ", record.valEcon, Color(0xFFFBBF24), Icons.Default.Paid)
                            MetricPill("Psych", record.valPsych, Color(0xFFC084FC), Icons.Default.Face)
                            MetricPill("Eco", record.valEco, Color(0xFF34D399), Icons.Default.Spa)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricPill("Law", record.valLaw, Color(0xFFFB7185), Icons.Default.Gavel)
                            MetricPill("Sec", record.valSec, Color(0xFFEF4444), Icons.Default.Security)
                            MetricPill("Phys", record.valPhys, Color(0xFFFB923C), Icons.Default.Speed)
                            MetricPill("Soc", record.valSoc, Color(0xFFF472B6), Icons.Default.Groups)
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

@Composable
fun MetricPill(label: String, value: Float, color: Color, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = OmnisBgDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, color = OmnisTextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${(value * 100).toInt()}%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun MatrixView(
    latestRecord: OmnisRecord?,
    simSys: Float,
    simEcon: Float,
    simPsych: Float,
    simEco: Float,
    simLaw: Float,
    simSec: Float,
    simPhys: Float,
    simSoc: Float,
    onSimChange: (Float, Float, Float, Float, Float, Float, Float, Float) -> Unit
) {
    val composite = (simSys + simEcon + simPsych + simEco + simLaw + simSec + simPhys + simSoc) / 8f
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
                        text = "OSMIDIMENZIONÁLNÍ MATICE DOPADŮ",
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
                        text = "Systémová rovnováha napříč 8 transdisciplinárními doménami.",
                        color = OmnisTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 8 Dimension Interactive Sliders
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
                title = "Systémové inženýrství & Kybernetika",
                desc = "Modularita, robustnost a čistota architektury",
                value = simSys,
                color = Color(0xFF60A5FA),
                icon = Icons.Default.Settings,
                testTag = "slider_sys",
                onValueChange = { onSimChange(it, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Teorie her & Ekonomie",
                desc = "Efektivita nákladů a návratnost investice",
                value = simEcon,
                color = Color(0xFFFBBF24),
                icon = Icons.Default.Paid,
                testTag = "slider_econ",
                onValueChange = { onSimChange(simSys, it, simPsych, simEco, simLaw, simSec, simPhys, simSoc) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Kognitivní vědy & Psychologie",
                desc = "Etika, transparentnost a důvěra operátora",
                value = simPsych,
                color = Color(0xFFC084FC),
                icon = Icons.Default.Face,
                testTag = "slider_psych",
                onValueChange = { onSimChange(simSys, simEcon, it, simEco, simLaw, simSec, simPhys, simSoc) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Regenerativní Ekologie",
                desc = "Udržitelnost a regenerativní potenciál biosféry",
                value = simEco,
                color = Color(0xFF34D399),
                icon = Icons.Default.Spa,
                testTag = "slider_eco",
                onValueChange = { onSimChange(simSys, simEcon, simPsych, it, simLaw, simSec, simPhys, simSoc) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Regulace & Právo",
                desc = "Soulad s legislativou a normami",
                value = simLaw,
                color = Color(0xFFFB7185),
                icon = Icons.Default.Gavel,
                testTag = "slider_law",
                onValueChange = { onSimChange(simSys, simEcon, simPsych, simEco, it, simSec, simPhys, simSoc) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Zero-Trust Bezpečnost",
                desc = "Ochrana perimetru a mitigace rizik",
                value = simSec,
                color = Color(0xFFEF4444),
                icon = Icons.Default.Security,
                testTag = "slider_sec",
                onValueChange = { onSimChange(simSys, simEcon, simPsych, simEco, simLaw, it, simPhys, simSoc) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Fyzikální termodynamika",
                desc = "Energetická entropie a fyzikální mantinely",
                value = simPhys,
                color = Color(0xFFFB923C),
                icon = Icons.Default.Speed,
                testTag = "slider_phys",
                onValueChange = { onSimChange(simSys, simEcon, simPsych, simEco, simLaw, simSec, it, simSoc) }
            )
        }

        item {
            DimensionSliderCard(
                title = "Socio-kulturní dynamika",
                desc = "Dopad na kulturní a sociální struktury",
                value = simSoc,
                color = Color(0xFFF472B6),
                icon = Icons.Default.Groups,
                testTag = "slider_soc",
                onValueChange = { onSimChange(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, it) }
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
    icon: ImageVector,
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
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
