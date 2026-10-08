package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OmnisRecord
import com.example.ui.OmnisNavigationState
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
    val userExperienceMode by viewModel.userExperienceMode.collectAsStateWithLifecycle()
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
    val telemetryLogs by viewModel.telemetry.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSmartHelp by remember { mutableStateOf(false) }

    if (showSmartHelp) {
        com.example.ui.OmnisSmartHelpDialog(onDismiss = { showSmartHelp = false })
    }

    var lastBackPressTimestamp by remember { mutableLongStateOf(0L) }

    // Android Hardware / Gesture Back Handling pro Admina (Plynulý zpětný krok napříč celou aplikací)
    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (viewModel.popTab()) {
            // Úspěšný návrat na předchozí okno v historii
        } else {
            // Uživatel je na domovské záložce a historie je prázdná -> Ochrana před nechtěným zavřením (Double Back to Exit)
            val now = System.currentTimeMillis()
            if (now - lastBackPressTimestamp < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTimestamp = now
                android.widget.Toast.makeText(
                    context,
                    "Stiskněte Zpět ještě jednou pro ukončení O.M.N.I.S.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
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
        @OptIn(ExperimentalLayoutApi::class)
        val isImeVisible = WindowInsets.isImeVisible

        Scaffold(
            topBar = {
                AdminTopAppBar(
                    activeTab = activeTab,
                    isCircuitBreakerTripped = isCircuitBreakerTripped,
                    isPromptGatewayEnabled = isPromptGatewayEnabled,
                    activeThreadTitle = activeThreadTitle,
                    userExperienceMode = userExperienceMode,
                    syncStatus = viewModel.cloudSqlSyncStatus.collectAsStateWithLifecycle().value,
                    onSyncClick = { viewModel.triggerCloudSqlSync() },
                    onToggleUserMode = { viewModel.toggleUserExperienceMode() },
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
                    onExportCertifiedAuditClick = {
                        val username = com.example.auth.OmnisAuthService.currentSession.value?.username ?: "admin"
                        com.example.ui.PdfExportEngine.exportCertifiedAuditReport(
                            context = context,
                            operatorName = username,
                            records = records,
                            telemetryLogs = telemetryLogs,
                            packageName = context.packageName
                        )
                    },
                    onHelpClick = { showSmartHelp = true }
                )
            },
            bottomBar = {
                if (!isImeVisible) {
                    com.example.ui.OmnisBottomNavigationBar(
                        activeTab = activeTab,
                        userRole = com.example.auth.UserRole.ADMIN_OPERATOR,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = if (isImeVisible) 0.dp else innerPadding.calculateBottomPadding()
                    )
                    .imePadding()
            )
        }
    }
}
