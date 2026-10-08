package com.example.ui.admin

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.TestSemanticChatView
import com.example.action.ActionExecutionResult
import com.example.auth.UserRole
import com.example.data.OmnisRecord
import com.example.ui.*
import com.example.ui.chat.ChatView
import com.example.ui.theme.OmnisBgDark
import com.example.ui.theme.OmnisBorderDark
import com.example.ui.theme.OmnisCyan
import com.example.ui.theme.OmnisPanelDark

/**
 * Striktně administrátorský router záložek v O.M.N.I.S.
 * Obsahuje výhradně systémové, diagnostické a vývojářské obrazovky.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTabRouter(
    activeTab: OmnisTab,
    records: List<OmnisRecord>,
    isLoading: Boolean,
    inputQuery: String,
    lastActionResult: ActionExecutionResult?,
    isActionExecuting: Boolean,
    actionLogs: List<String>,
    fragments: List<com.example.data.MemoryFragment>,
    isConsolidating: Boolean,
    onConsolidateMemory: () -> Unit,
    onDeleteMemoryFragment: (Long) -> Unit,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit,
    viewModel: OmnisViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(targetState = activeTab, modifier = modifier.fillMaxSize(), label = "admin_tab_transition") { currentTab ->
        when (currentTab) {
            OmnisTab.ADMIN -> {
                val isHealing by viewModel.isSelfHealingActive.collectAsStateWithLifecycle()
                val selfHealingLogs by viewModel.selfHealingLogs.collectAsStateWithLifecycle()
                val cloudSqlSyncStatus by viewModel.cloudSqlSyncStatus.collectAsStateWithLifecycle()

                AdminHubView(
                    records = records,
                    onExecuteAction = { payload -> viewModel.executeActionPayload(payload) },
                    isRunningAction = isActionExecuting,
                    actionLogs = actionLogs,
                    fragments = fragments,
                    isConsolidating = isConsolidating,
                    onConsolidateMemory = onConsolidateMemory,
                    onDeleteMemoryFragment = onDeleteMemoryFragment,
                    onNavigateToChat = { prompt ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    },
                    onPurgeSyncedRecords = { syncedCount ->
                        viewModel.purgeSyncedLocalRecords(syncedCount)
                    },
                    isHealing = isHealing,
                    selfHealingLogs = selfHealingLogs,
                    onTriggerHealing = { viewModel.performSelfHealing() },
                    syncStatus = cloudSqlSyncStatus,
                    onSyncCloudSql = { viewModel.triggerCloudSqlSync() },
                    onTestCloudSqlConnection = { viewModel.testCloudSqlConnection() }
                )
            }
            OmnisTab.NODES -> {
                CognitiveNodesView(
                    records = records,
                    onExecuteAction = { payload ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.executeActionPayload(payload)
                    },
                    onNavigateToChat = { prompt ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    },
                    onSaveArtifact = viewModel::onCreateArtifact,
                    onPurgeSyncedRecords = { count -> viewModel.purgeSyncedLocalRecords(count) }
                )
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
                    onSynthesize = { selectedIds -> viewModel.synthesizeSelectedRecords(selectedIds) },
                    onClearComparison = { viewModel.clearComparison() },
                    onDirectMitigate = { prompt ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    },
                    onCreateCompensatoryGoal = viewModel::createGoalWithTasks
                )
            }
            OmnisTab.TELEMETRY -> {
                val telemetryLogs by viewModel.telemetry.collectAsStateWithLifecycle()
                com.example.ui.telemetry.TelemetryDashboardView(
                    logs = telemetryLogs,
                    onClearLogs = viewModel::onClearTelemetry
                )
            }
            OmnisTab.GUIDE -> {
                com.example.ui.guide.MethodologyView(userRole = UserRole.ADMIN_OPERATOR)
            }
            OmnisTab.TEST_SEMANTIC -> {
                TestSemanticChatView(
                    records = emptyList(),
                    isLoading = isLoading,
                    inputQuery = inputQuery,
                    onQueryChange = viewModel::onQueryChange,
                    onSend = { viewModel.sendQuery() },
                    onWeightChange = { _, _, _, _ -> },
                    onReSynthesize = { _, _ -> }
                )
            }
            OmnisTab.DEV_PROMPT_LAB -> {
                val telemetryLogs by viewModel.telemetry.collectAsStateWithLifecycle()
                val llmAnalysisResult by viewModel.llmAnalysisResult.collectAsStateWithLifecycle()
                val isAnalyzingError by viewModel.isAnalyzingError.collectAsStateWithLifecycle()
                val isPromptGatewayEnabled by viewModel.isPromptGatewayEnabled.collectAsStateWithLifecycle()
                val promptGatewayThreshold by viewModel.promptGatewayThreshold.collectAsStateWithLifecycle()
                DevPromptLab(
                    telemetryLogs = telemetryLogs,
                    llmAnalysisResult = llmAnalysisResult,
                    isAnalyzingError = isAnalyzingError,
                    isPromptGatewayEnabled = isPromptGatewayEnabled,
                    promptGatewayThreshold = promptGatewayThreshold,
                    onTogglePromptGateway = viewModel::togglePromptGateway,
                    onThresholdChange = viewModel::setPromptGatewayThreshold,
                    onClearTelemetry = viewModel::onClearTelemetry,
                    onSimulateDiagnosticError = viewModel::logDiagnosticWarning,
                    onAnalyzeErrorWithLlm = viewModel::analyzeErrorWithLlm,
                    onClearLlmAnalysis = viewModel::clearLlmErrorAnalysis
                )
            }
            OmnisTab.DASHBOARD -> {
                val goals by viewModel.goals.collectAsStateWithLifecycle()
                val artifacts by viewModel.artifacts.collectAsStateWithLifecycle()
                com.example.ui.cockpit.OmnisCockpitView(
                    latestRecord = records.firstOrNull { it.role == "assistant" },
                    activeGoals = goals.filter { it.status == "ACTIVE" },
                    recentArtifacts = artifacts,
                    onTabSwitch = viewModel::setTab,
                    onSendToChat = { prompt ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    },
                    onCreateGoal = viewModel::onCreateGoal
                )
            }
            OmnisTab.CHAT -> {
                val selectedDomains by viewModel.selectedDomains.collectAsStateWithLifecycle()
                val focusedDomain by viewModel.focusedDomain.collectAsStateWithLifecycle()
                val selectedRecord by viewModel.selectedRecordForDetail.collectAsStateWithLifecycle()
                val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()
                val streamState by viewModel.streamState.collectAsStateWithLifecycle()
                val activeMemoryFragments by viewModel.activeMemoryFragments.collectAsStateWithLifecycle()
                val isPromptGuideEnabled by viewModel.isPromptGuideEnabled.collectAsStateWithLifecycle()
                val isCircuitBreakerTripped by viewModel.isCircuitBreakerTripped.collectAsStateWithLifecycle()
                val isPromptGatewayEnabled by viewModel.isPromptGatewayEnabled.collectAsStateWithLifecycle()
                val selectedDomain by viewModel.selectedDomain.collectAsStateWithLifecycle()
                val userExperienceMode by viewModel.userExperienceMode.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    ChatView(
                        records = records,
                        isLoading = isLoading,
                        isOcrLoading = isOcrLoading,
                        streamState = streamState,
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
                        onAuthorizeRecord = viewModel::authorizeBlockedRecord,
                        onClearDomainSelection = viewModel::clearSelection,
                        onMultiDomainSynthesis = { mode -> viewModel.runMultiDomainSynthesis(mode) },
                        onExecuteActionPayload = { payload -> viewModel.executeActionPayload(payload) },
                        lastActionResult = lastActionResult,
                        isActionExecuting = isActionExecuting,
                        userRole = UserRole.ADMIN_OPERATOR,
                        activeMemoryFragments = activeMemoryFragments,
                        isPromptGuideEnabled = isPromptGuideEnabled,
                        onDisablePromptGuide = { viewModel.setPromptGuideEnabled(false) },
                        isCircuitBreakerTripped = isCircuitBreakerTripped,
                        onToggleCircuitBreaker = viewModel::toggleCircuitBreaker,
                        isPromptGatewayEnabled = isPromptGatewayEnabled,
                        onTogglePromptGateway = viewModel::togglePromptGateway,
                        currentDomain = selectedDomain,
                        onDomainChange = viewModel::onDomainChange,
                        userExperienceMode = userExperienceMode,
                        onToggleUserMode = viewModel::toggleUserExperienceMode
                    )

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
            OmnisTab.SCENARIOS -> {
                com.example.ui.scenarios.ScenarioPlannerView(
                    currentRecord = records.lastOrNull { it.role == "assistant" },
                    onSaveArtifact = viewModel::onCreateArtifact,
                    onCreateGoal = viewModel::onCreateGoal,
                    onSendToChat = { prompt ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    }
                )
            }
            OmnisTab.GOALS -> {
                val goals by viewModel.goals.collectAsStateWithLifecycle()
                com.example.ui.goals.GoalTrackerView(
                    goals = goals,
                    onCreateGoal = viewModel::onCreateGoal,
                    onDeleteGoal = viewModel::onDeleteGoal,
                    onToggleTask = viewModel::onToggleGoalTask,
                    onSaveArtifact = viewModel::onCreateArtifact,
                    onSendToChat = { prompt ->
                        viewModel.onQueryChange(prompt)
                        viewModel.setTab(OmnisTab.CHAT)
                    }
                )
            }
            OmnisTab.ARTIFACTS -> {
                val artifacts by viewModel.artifacts.collectAsStateWithLifecycle()
                com.example.ui.artifacts.ArtifactGalleryView(
                    artifacts = artifacts,
                    onDeleteArtifact = viewModel::onDeleteArtifact,
                    onExportArtifact = viewModel::onExportArtifact,
                    onCreateArtifact = viewModel::onCreateArtifact,
                    onSendToChat = { prompt ->
                        viewModel.onQueryChange(prompt)
                        viewModel.setTab(OmnisTab.CHAT)
                    }
                )
            }
            OmnisTab.NEXUS -> {
                val activeAgentIds by viewModel.activeNexusAgents.collectAsStateWithLifecycle()
                var nexusMode by remember { mutableIntStateOf(0) }

                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = nexusMode,
                        containerColor = OmnisPanelDark,
                        contentColor = OmnisCyan
                    ) {
                        Tab(
                            selected = nexusMode == 0,
                            onClick = { nexusMode = 0 },
                            text = { Text("Topologie Sítě", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = nexusMode == 1,
                            onClick = { nexusMode = 1 },
                            text = { Text("Agentní Dialog", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    if (nexusMode == 0) {
                        com.example.ui.nexus.NexusVisualizerView(activeAgentIds = activeAgentIds)
                    } else {
                        com.example.ui.nexus.NexusView(
                            query = inputQuery,
                            onQueryChange = viewModel::onQueryChange,
                            onRunNexus = { agents -> viewModel.runNexusCollaboration(inputQuery, agents) },
                            onSaveArtifact = viewModel::onCreateArtifact,
                            onCreateGoal = viewModel::onCreateGoal,
                            onSendToChat = { prompt ->
                                viewModel.onQueryChange(prompt)
                                viewModel.setTab(OmnisTab.CHAT)
                            }
                        )
                    }
                }
            }
            OmnisTab.MEMORY -> {
                MemoryView(
                    records = records,
                    onItemClick = { record ->
                        viewModel.jumpToContext(record)
                    }
                )
            }
            OmnisTab.ANALYTICS -> {
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
                    onSynthesize = { selectedIds -> viewModel.synthesizeSelectedRecords(selectedIds) },
                    onClearComparison = { viewModel.clearComparison() },
                    onDirectMitigate = { prompt ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    },
                    onCreateCompensatoryGoal = viewModel::createGoalWithTasks
                )
            }
            OmnisTab.CAUSAL_SIMULATOR -> {
                val latestRecord = records.lastOrNull { it.role == "assistant" }
                com.example.ui.causal.CausalSimulatorView(
                    baselineRecord = latestRecord,
                    onSaveArtifact = viewModel::onCreateArtifact,
                    onCreateGoal = viewModel::onCreateGoal,
                    onSendToChat = { prompt ->
                        viewModel.onQueryChange(prompt)
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    }
                )
            }
            OmnisTab.DIALECTICS -> {
                com.example.ui.dialectics.DialecticEngineView(
                    onSendToChat = { prompt ->
                        viewModel.onQueryChange(prompt)
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    }
                )
            }
            OmnisTab.PRODUCTION_AUDIT -> {
                com.example.ui.audit.ProductionAuditView(
                    onPruneOldData = {
                        viewModel.pruneOldDatabaseRecords(daysOld = 14)
                    }
                )
            }
        }
    }
}
