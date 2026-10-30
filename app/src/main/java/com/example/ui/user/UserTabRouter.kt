package com.example.ui.user

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
 * Striktně uživatelský router záložek v O.M.N.I.S.
 * Obsahuje výhradně kognitivní, konverzační a analytické obrazovky určené pro běžné uživatele.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTabRouter(
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
    onRunNexusCollaboration: (String, List<String>) -> Unit,
    viewModel: OmnisViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(targetState = activeTab, modifier = modifier.fillMaxSize(), label = "user_tab_transition") { currentTab ->
        when (currentTab) {
            OmnisTab.DASHBOARD -> {
                val goals by viewModel.goals.collectAsStateWithLifecycle()
                val artifacts by viewModel.artifacts.collectAsStateWithLifecycle()
                val isEcoMode by viewModel.isEcoMode.collectAsStateWithLifecycle()
                com.example.ui.cockpit.OmnisCockpitView(
                    latestRecord = records.firstOrNull { it.role == "assistant" },
                    activeGoals = goals.filter { it.status == "ACTIVE" },
                    recentArtifacts = artifacts,
                    onTabSwitch = viewModel::setTab,
                    onSendToChat = { prompt ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
                    },
                    onCreateGoal = viewModel::onCreateGoal,
                    isEcoMode = isEcoMode,
                    onToggleEcoMode = viewModel::toggleEcoMode
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
                    ChatView(viewModel = viewModel)

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
                            onRunNexus = { agents -> onRunNexusCollaboration(inputQuery, agents) },
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
            OmnisTab.GUIDE -> {
                com.example.ui.guide.MethodologyView(userRole = UserRole.STANDARD_USER)
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
            OmnisTab.SCENARIOS -> {
                val records by viewModel.records.collectAsStateWithLifecycle()
                com.example.ui.scenarios.ScenarioPlannerView(
                    currentRecord = records.firstOrNull(),
                    onSaveArtifact = viewModel::onCreateArtifact,
                    onCreateGoal = viewModel::onCreateGoal,
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
            OmnisTab.MEMORY -> {
                MemoryView(
                    records = records,
                    onItemClick = { record ->
                        viewModel.jumpToContext(record)
                    },
                    fragments = fragments,
                    onInjectContext = { frag ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery("Injektovat sémantický kontext paměti [${frag.title}]: ${frag.summary}")
                    }
                )
            }
            OmnisTab.ANALYTICS -> {
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
            OmnisTab.SETTINGS -> {
                val userExperienceMode by viewModel.userExperienceMode.collectAsStateWithLifecycle()
                com.example.ui.settings.SettingsView(
                    userExperienceMode = userExperienceMode,
                    onExperienceModeChange = viewModel::setUserExperienceMode,
                    onSpeak = onSpeak
                )
            }
            else -> {
                // Výchozí bezpečný návrat do chatu pokud uživatel zkusil administrátorskou nebo neautorizovanou záložku
                viewModel.setTab(OmnisTab.CHAT)
            }
        }
    }
}
