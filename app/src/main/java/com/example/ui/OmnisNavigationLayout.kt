package com.example.ui

import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.OmnisRecord
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.OctagonDashboard
import com.example.TestSemanticChatView
import com.example.Greeting
import com.example.ui.PdfExportEngine
import com.example.ui.SpeechController
import com.example.ui.OmnisNavigationState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OmnisMainScreen(
                    
    viewModel: OmnisViewModel,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()

    if (!isAuthenticated) {
        OmnisLoginScreen(
            onLoginSuccess = {
                // Auth stav je automaticky aktualizován v OmnisAuthService a OmnisViewModel
            }
        )
        return
    }

    val isCircuitBreakerTripped by viewModel.isCircuitBreakerTripped.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputQuery by viewModel.inputQuery.collectAsStateWithLifecycle()
    val ocrValidationState by viewModel.ocrValidationState.collectAsStateWithLifecycle()
    val pendingGatewayReview by viewModel.pendingGatewayReview.collectAsStateWithLifecycle()
    
    val navState = remember { com.example.ui.OmnisNavigationState() }
    val showDeleteConfirm by navState.showDeleteConfirm.collectAsStateWithLifecycle()
    val isActionExecuting by viewModel.isActionExecuting.collectAsStateWithLifecycle()
    val lastActionResult by viewModel.lastActionResult.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Password-Protected Admin Switch State
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var isAdminPasswordError by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    ocrValidationState?.let { state ->
        com.example.ui.OcrValidationDialog(
            state = state,
            onTextChanged = viewModel::updateOcrValidationText,
            onConfirm = viewModel::confirmOcrValidation,
            onCancel = viewModel::cancelOcrValidation
        )
    }

    pendingGatewayReview?.let { reviewState ->
        com.example.ui.PromptGatewayReviewModal(
            reviewState = reviewState,
            onConfirmed = { optimizedPrompt ->
                viewModel.confirmGatewayReview(optimizedPrompt)
            },
            onBypassWithOriginal = {
                viewModel.bypassGatewayReview()
            },
            onDismiss = {
                viewModel.dismissGatewayReview()
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { navState.setShowDeleteConfirm(false) },
            title = { Text("Potvrdit smazání") },
            text = { Text("Opravdu chcete vymazat celou historii paměti O.M.N.I.S.? Tato akce je nevratná.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        navState.setShowDeleteConfirm(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Smazat", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { navState.setShowDeleteConfirm(false) }) { Text("Zrušit") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            OmnisDrawerContent(
                activeTab = activeTab,
                currentRole = currentRole,
                isCircuitBreakerTripped = isCircuitBreakerTripped,
                onTabSelected = { tab ->
                    viewModel.setTab(tab)
                    scope.launch { drawerState.close() }
                },
                onToggleCircuitBreaker = { viewModel.toggleCircuitBreaker() },
                onSwitchToAdminClick = { showAdminPasswordDialog = true },
                onLogoutClick = {
                    viewModel.logout()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        // Admin Password Dialog
        if (showAdminPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                        showAdminPasswordDialog = false
                        adminPasswordInput = ""
                        isAdminPasswordError = false
                    },
                    containerColor = OmnisPanelDark,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = OmnisAmber)
                            Text("Administrátorský Režim", color = OmnisAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            Text("Zadejte přístupový klíč pro povýšení oprávnění na Admin / Operátor:", color = OmnisTextMuted, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = adminPasswordInput,
                                onValueChange = {
                                    adminPasswordInput = it
                                    isAdminPasswordError = false
                                },
                                label = { Text("Heslo admina") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                isError = isAdminPasswordError,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OmnisAmber,
                                    unfocusedBorderColor = OmnisBorderDark,
                                    errorBorderColor = Color.Red
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (isAdminPasswordError) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Neplatný přístupový klíč operátora.", color = Color.Red, fontSize = 10.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val success = com.example.auth.OmnisAuthService.login(context, "admin", adminPasswordInput, com.example.auth.UserRole.ADMIN_OPERATOR)
                                if (success) {
                                    showAdminPasswordDialog = false
                                    adminPasswordInput = ""
                                    isAdminPasswordError = false
                                    scope.launch { drawerState.close() }
                                    Toast.makeText(context, "Oprávnění úspěšně povýšena na Admin / Operátor", Toast.LENGTH_SHORT).show()
                                } else {
                                    isAdminPasswordError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OmnisAmber)
                        ) {
                            Text("Aktivovat Admin", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showAdminPasswordDialog = false
                                adminPasswordInput = ""
                                isAdminPasswordError = false
                            }
                        ) {
                            Text("Zrušit", color = OmnisTextMuted)
                        }
                    }
                )
            }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(OmnisBgDark),
            containerColor = OmnisBgDark,
            topBar = {
                OmnisTopAppBar(
                    records = records,
                    onOpenMenu = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onClearSessionClick = { navState.setShowDeleteConfirm(true) }
                )
            }
    ) { innerPadding ->
        OmnisTabRouter(
            activeTab = activeTab,
            records = records,
            isLoading = isLoading,
            inputQuery = inputQuery,
            lastActionResult = lastActionResult,
            isActionExecuting = isActionExecuting,
            currentRole = currentRole,
            onSpeak = onSpeak,
            onExportPdf = onExportPdf,
            viewModel = viewModel,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        )
    }
}
}

@Composable
fun ChatView(
    records: List<OmnisRecord>,
    isLoading: Boolean,
    isOcrLoading: Boolean = false,
    streamState: OmnisViewModel.StreamState? = null,
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
    onAuthorizeRecord: (OmnisRecord) -> Unit = {},
    onClearDomainSelection: () -> Unit = {},
    onMultiDomainSynthesis: (String) -> Unit = {},
    onExecuteActionPayload: (com.example.action.ActionPayload) -> Unit = {},
    lastActionResult: com.example.action.ActionExecutionResult? = null,
    isActionExecuting: Boolean = false,
    userRole: com.example.auth.UserRole = com.example.auth.UserRole.ADMIN_OPERATOR
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

    LaunchedEffect(records.size, isLoading, streamState?.stage) {
        if (records.isNotEmpty() && scrollToId == null) {
            val targetIndex = if (isLoading || streamState != null) records.size else records.size - 1
            listState.animateScrollToItem(targetIndex)
        } else if (records.isEmpty() && (isLoading || streamState != null)) {
            listState.animateScrollToItem(0)
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

            if (isLoading && streamState != null) {
                item {
                    StreamingCognitiveMessage(streamState)
                }
            } else if (isLoading) {
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

        // Multi-Domain Synthesis Action Bar
        if (selectedDomains.isNotEmpty()) {
            val clusterAnalysis = remember(selectedDomains) {
                OmnisCorrelationEngine.evaluateCluster(selectedDomains)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = OmnisPanelDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (clusterAnalysis.hasFriction) OmnisAmber else OmnisCyan
                ),
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (clusterAnalysis.hasFriction) OmnisAmber else OmnisCyan)
                                )
                                Text(
                                    text = "SYNTÉZNÍ PANEL (${selectedDomains.size})",
                                    color = if (clusterAnalysis.hasFriction) OmnisAmber else OmnisCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "• Synergie: ${(clusterAnalysis.averageSynergy * 100).toInt()}%",
                                    color = OmnisTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = selectedDomains.joinToString(", "),
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onClearDomainSelection,
                                modifier = Modifier.testTag("synthesis_cancel_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Zrušit", color = OmnisTextMuted, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onMultiDomainSynthesis("COMPARE") },
                                modifier = Modifier.testTag("synthesis_compare_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = OmnisBgDark),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Srovnat", color = Color.White, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onMultiDomainSynthesis("HARMONIZE") },
                                modifier = Modifier.testTag("synthesis_harmonize_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Harmonizovat", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Interference & Synergy Dynamic Diagnosis
                    if (selectedDomains.size >= 2) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = clusterAnalysis.summary,
                            color = if (clusterAnalysis.hasFriction) OmnisAmber else OmnisCyan.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Action-Driven Reactive Operational Panel (Jen pro roli Admin / Operátor)
        if (userRole.canAccessSystemActions()) {
            ActionDrivenInteractivePanel(
                onExecuteActionPayload = onExecuteActionPayload,
                lastActionResult = lastActionResult,
                isActionExecuting = isActionExecuting
            )
        }

        // Input Bar
        Surface(
            color = OmnisPanelDark,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                    modifier = Modifier
                        .size(40.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Přiložit soubor", tint = OmnisCyan, modifier = Modifier.size(20.dp))
                }

                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, 
                    modifier = Modifier
                        .size(40.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Vyfotit/Obrázek", tint = OmnisCyan, modifier = Modifier.size(20.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .background(OmnisBgDark, RoundedCornerShape(18.dp))
                        .border(1.dp, OmnisBorderDark, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = inputQuery,
                        onValueChange = onQueryChange,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 22.dp, max = 120.dp)
                            .testTag("chat_input_field"),
                        maxLines = 4,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(OmnisCyan),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    if (inputQuery.isEmpty()) {
                                        Text("Zpráva pro O.M.N.I.S...", color = OmnisTextMuted, fontSize = 13.sp)
                                    }
                                    innerTextField()
                                }
                                IconButton(
                                    onClick = {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "cs-CZ")
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diktujte dotaz...")
                                        }
                                        speechLauncher.launch(intent)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Diktovat hlasem", tint = OmnisCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )
                    
                    if (isOcrLoading) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(OmnisBgDark.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .border(1.dp, OmnisCyan, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(color = OmnisCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("OCR zpracování...", color = OmnisCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onSend,
                    enabled = inputQuery.isNotBlank() && !isLoading && !isOcrLoading,
                    modifier = Modifier
                        .size(42.dp)
                        .minimumInteractiveComponentSize()
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
                        modifier = Modifier.size(20.dp)
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OmnisBgDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("attached_artifact_container")
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = OmnisCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "ARTEFAKT: ${file.name}",
                                    color = OmnisCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (file.exists()) {
                                    val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                                    Text(
                                        text = "${sizeKb} KB",
                                        color = OmnisTextMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            if (file.exists()) {
                                coil.compose.AsyncImage(
                                    model = file,
                                    contentDescription = "Připojený obrázek",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                        }
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                MetricPill("Sys", record.valSys, Color(0xFF60A5FA), Icons.Default.Settings, selectedDomains.contains("Sys"), onClick = { onDomainClick("Sys") }, onLongClick = { onDomainLongClick("Sys", record) })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MetricPill("Econ", record.valEcon, Color(0xFFFBBF24), Icons.Default.Paid, selectedDomains.contains("Econ"), onClick = { onDomainClick("Econ") }, onLongClick = { onDomainLongClick("Econ", record) })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MetricPill("Psych", record.valPsych, Color(0xFFC084FC), Icons.Default.Face, selectedDomains.contains("Psych"), onClick = { onDomainClick("Psych") }, onLongClick = { onDomainLongClick("Psych", record) })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MetricPill("Eco", record.valEco, Color(0xFF34D399), Icons.Default.Spa, selectedDomains.contains("Eco"), onClick = { onDomainClick("Eco") }, onLongClick = { onDomainLongClick("Eco", record) })
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                MetricPill("Law", record.valLaw, Color(0xFFFB7185), Icons.Default.Gavel, selectedDomains.contains("Law"), onClick = { onDomainClick("Law") }, onLongClick = { onDomainLongClick("Law", record) })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MetricPill("Sec", record.valSec, Color(0xFFEF4444), Icons.Default.Security, selectedDomains.contains("Sec"), onClick = { onDomainClick("Sec") }, onLongClick = { onDomainLongClick("Sec", record) })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MetricPill("Phys", record.valPhys, Color(0xFFFB923C), Icons.Default.Speed, selectedDomains.contains("Phys"), onClick = { onDomainClick("Phys") }, onLongClick = { onDomainLongClick("Phys", record) })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MetricPill("Soc", record.valSoc, Color(0xFFF472B6), Icons.Default.Groups, selectedDomains.contains("Soc"), onClick = { onDomainClick("Soc") }, onLongClick = { onDomainLongClick("Soc", record) })
                            }
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
    val percent = (value * 100).toInt()
    val stateDescription = if (isSelected) "Vybráno pro syntézu" else "Nevybráno"
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color.copy(alpha = 0.25f) else OmnisBgDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else color.copy(alpha = 0.4f)),
        modifier = Modifier
            .testTag("metric_pill_${label.lowercase()}")
            .semantics {
                this.contentDescription = "Doména $label: $percent procent. $stateDescription. Dlouhým stiskem zobrazíte detail."
                this.selected = isSelected
            }
            .minimumInteractiveComponentSize()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = if (isSelected) color else color.copy(alpha = 0.85f), 
                modifier = Modifier.size(16.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label, 
                    color = if (isSelected) Color.White else OmnisTextMuted, 
                    fontSize = 10.sp, 
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "$percent%",
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

@Composable
fun StreamingCognitiveMessage(streamState: OmnisViewModel.StreamState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 2.dp,
                bottomEnd = 16.dp
            ),
            color = OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(0.96f)
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = OmnisCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when(streamState.stage) {
                                "introspection" -> "O.M.N.I.S. CORE (Introspekce...)"
                                "execution" -> "O.M.N.I.S. CORE (Generování...)"
                                "verification" -> "O.M.N.I.S. CORE (Verifikováno)"
                                else -> "O.M.N.I.S. CORE (Zpracovávám...)"
                            },
                            color = if (streamState.stage == "verification") OmnisCyan else OmnisCyan.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (streamState.status == "MULTI-LAYER VERIFIED" && streamState.score != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = OmnisCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(12.dp))
                            Text(
                                text = "MULTI-LAYER VERIFIED (${(streamState.score * 100).toInt()}%)",
                                color = OmnisCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisBgDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (streamState.stage != "verification") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = OmnisCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = OmnisCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "Kognitivní introspekce",
                                    color = OmnisCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = OmnisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (streamState.stage == "verification") "Syntéza úspěšně dokončena. Čekám na vykreslení výsledku." else streamState.text,
                            color = OmnisTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}