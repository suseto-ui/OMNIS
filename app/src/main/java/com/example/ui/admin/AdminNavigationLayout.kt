package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OmnisRecord
import com.example.ui.OmnisNavigationState
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.theme.OmnisBgDark
import kotlinx.coroutines.launch

/**
 * Hlavní administrátorské rozhraní (Admin Scaffold & Navigation Layout).
 * Fyzicky a architektonicky izolováno od uživatelského rozhraní.
 */
@Composable
fun AdminNavigationLayout(
    viewModel: OmnisViewModel,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isCircuitBreakerTripped by viewModel.isCircuitBreakerTripped.collectAsStateWithLifecycle()
    val isPromptGatewayEnabled by viewModel.isPromptGatewayEnabled.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputQuery by viewModel.inputQuery.collectAsStateWithLifecycle()
    val ocrValidationState by viewModel.ocrValidationState.collectAsStateWithLifecycle()
    val pendingGatewayReview by viewModel.pendingGatewayReview.collectAsStateWithLifecycle()

    val navState = remember { OmnisNavigationState() }
    val showDeleteConfirm by navState.showDeleteConfirm.collectAsStateWithLifecycle()
    val isActionExecuting by viewModel.isActionExecuting.collectAsStateWithLifecycle()
    val actionLogs by viewModel.actionLogs.collectAsStateWithLifecycle()
    val lastActionResult by viewModel.lastActionResult.collectAsStateWithLifecycle()
    val memoryFragments by viewModel.memoryFragments.collectAsStateWithLifecycle()
    val isConsolidating by viewModel.isConsolidating.collectAsStateWithLifecycle()
    val availableThreads by viewModel.availableThreads.collectAsStateWithLifecycle()
    val activeThreadId by viewModel.activeThreadId.collectAsStateWithLifecycle()
    val activeThreadTitle by viewModel.activeThreadTitle.collectAsStateWithLifecycle()
    val allUserNames by viewModel.allUserNames.collectAsStateWithLifecycle()
    val adminSelectedUserFilter by viewModel.adminSelectedUserFilter.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSmartHelp by remember { mutableStateOf(false) }

    if (showSmartHelp) {
        com.example.ui.OmnisSmartHelpDialog(onDismiss = { showSmartHelp = false })
    }

    // Android Hardware / Gesture Back Handling pro Admina
    BackHandler(enabled = drawerState.isOpen || viewModel.canNavigateBack) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.popTab()
        }
    }

    // OCR Validation Dialog
    ocrValidationState?.let { state ->
        com.example.ui.OcrValidationDialog(
            state = state,
            onTextChanged = viewModel::updateOcrValidationText,
            onConfirm = viewModel::confirmOcrValidation,
            onCancel = viewModel::cancelOcrValidation
        )
    }

    // Prompt Gateway Review Modal
    pendingGatewayReview?.let { reviewState ->
        com.example.ui.PromptGatewayReviewModal(
            reviewState = reviewState,
            onConfirmed = { optimizedPrompt -> viewModel.confirmGatewayReview(optimizedPrompt) },
            onBypassWithOriginal = { viewModel.bypassGatewayReview() },
            onDismiss = { viewModel.dismissGatewayReview() }
        )
    }

    // Modal Potvrzení Smazání Historie Paměti
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { navState.setShowDeleteConfirm(false) },
            title = { Text("Potvrdit smazání databáze") },
            text = { Text("Opravdu chcete vymazat celou databázi a historii paměti O.M.N.I.S.? Tato akce je nevratná.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        navState.setShowDeleteConfirm(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Smazat Vše", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { navState.setShowDeleteConfirm(false) }) { Text("Zrušit") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                activeTab = activeTab,
                isCircuitBreakerTripped = isCircuitBreakerTripped,
                isPromptGatewayEnabled = isPromptGatewayEnabled,
                availableThreads = availableThreads,
                activeThreadId = activeThreadId,
                allUserNames = allUserNames,
                adminSelectedUserFilter = adminSelectedUserFilter,
                onTabSelected = { tab ->
                    viewModel.setTab(tab)
                    scope.launch { drawerState.close() }
                },
                onSelectUserFilter = { filter ->
                    viewModel.setAdminUserFilter(filter)
                },
                onSelectThread = { threadId, title ->
                    viewModel.selectThread(threadId, title)
                    scope.launch { drawerState.close() }
                },
                onCreateNewThread = {
                    viewModel.createNewThread()
                    scope.launch { drawerState.close() }
                },
                onDeleteThread = { threadId ->
                    viewModel.deleteThread(threadId)
                },
                onRenameThread = { threadId, newTitle ->
                    viewModel.renameThread(threadId, newTitle)
                },
                onToggleCircuitBreaker = { viewModel.toggleCircuitBreaker() },
                onTogglePromptGateway = { viewModel.togglePromptGateway() },
                onLogoutClick = {
                    viewModel.logout()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    activeTab = activeTab,
                    isCircuitBreakerTripped = isCircuitBreakerTripped,
                    isPromptGatewayEnabled = isPromptGatewayEnabled,
                    activeThreadTitle = activeThreadTitle,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onDeleteHistoryClick = { navState.setShowDeleteConfirm(true) },
                    onNewThreadClick = { viewModel.createNewThread() },
                    onExportPdfClick = {
                        val username = com.example.auth.OmnisAuthService.currentSession.value?.username ?: "admin"
                        com.example.ui.PdfExportEngine.exportThreadToPdf(context, activeThreadTitle.ifBlank { "Vlákno" }, username, records, context.packageName)
                    },
                    onExportMarkdownClick = {
                        val username = com.example.auth.OmnisAuthService.currentSession.value?.username ?: "admin"
                        com.example.ui.PdfExportEngine.exportThreadToMarkdown(context, activeThreadTitle.ifBlank { "Vlákno" }, username, records, context.packageName)
                    },
                    onHelpClick = { showSmartHelp = true }
                )
            },
            containerColor = OmnisBgDark
        ) { innerPadding ->
            AdminTabRouter(
                activeTab = activeTab,
                records = records,
                isLoading = isLoading,
                inputQuery = inputQuery,
                lastActionResult = lastActionResult,
                isActionExecuting = isActionExecuting,
                actionLogs = actionLogs,
                fragments = memoryFragments,
                isConsolidating = isConsolidating,
                onConsolidateMemory = { viewModel.triggerMemoryConsolidation() },
                onDeleteMemoryFragment = { id -> viewModel.deleteMemoryFragment(id) },
                onSpeak = onSpeak,
                onExportPdf = onExportPdf,
                viewModel = viewModel,
                modifier = Modifier
                    .padding(innerPadding)
                    .imePadding()
            )
        }
    }
}
