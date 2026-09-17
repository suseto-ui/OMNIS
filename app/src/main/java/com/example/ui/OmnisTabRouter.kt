package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.action.ActionExecutionResult
import com.example.auth.UserRole
import com.example.data.OmnisRecord
import com.example.ui.theme.OmnisBgDark
import com.example.ui.theme.OmnisBorderDark

/**
 * Extrahovaný router pro záložky hlavního obsahu v O.M.N.I.S.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmnisTabRouter(
    activeTab: OmnisTab,
    records: List<OmnisRecord>,
    isLoading: Boolean,
    inputQuery: String,
    lastActionResult: ActionExecutionResult?,
    isActionExecuting: Boolean,
    currentRole: UserRole,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit,
    viewModel: OmnisViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (activeTab) {
            OmnisTab.CHAT -> {
                val selectedDomains by viewModel.selectedDomains.collectAsStateWithLifecycle()
                val focusedDomain by viewModel.focusedDomain.collectAsStateWithLifecycle()
                val selectedRecord by viewModel.selectedRecordForDetail.collectAsStateWithLifecycle()
                val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()
                val streamState by viewModel.streamState.collectAsStateWithLifecycle()

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
                        userRole = currentRole
                    )

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
                    },
                    onDirectMitigate = { prompt ->
                        viewModel.setTab(OmnisTab.CHAT)
                        viewModel.sendQuery(prompt)
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
                onClearComparison = { viewModel.clearComparison() },
                onDirectMitigate = { prompt ->
                    viewModel.setTab(OmnisTab.CHAT)
                    viewModel.sendQuery(prompt)
                }
            )
            OmnisTab.ADMIN -> {
                if (currentRole.canAccessSystemActions()) {
                    AdminHubView(
                        records = records,
                        onExecuteAction = { payload ->
                            viewModel.setTab(OmnisTab.CHAT)
                            viewModel.executeActionPayload(payload)
                        },
                        onNavigateToChat = { prompt ->
                            viewModel.setTab(OmnisTab.CHAT)
                            viewModel.sendQuery(prompt)
                        },
                        onPurgeSyncedRecords = { syncedCount ->
                            viewModel.purgeSyncedLocalRecords(syncedCount)
                        }
                    )
                } else {
                    OmnisUnauthorizedView(onSwitchRoleClick = { viewModel.setTab(OmnisTab.CHAT) })
                }
            }
            OmnisTab.NODES -> {
                if (currentRole.canAccessSystemActions()) {
                    CognitiveNodesView(
                        records = records,
                        onExecuteAction = { payload ->
                            viewModel.setTab(OmnisTab.CHAT)
                            viewModel.executeActionPayload(payload)
                        },
                        onNavigateToChat = { prompt ->
                            viewModel.setTab(OmnisTab.CHAT)
                            viewModel.sendQuery(prompt)
                        }
                    )
                } else {
                    OmnisUnauthorizedView(onSwitchRoleClick = { viewModel.setTab(OmnisTab.CHAT) })
                }
            }
            OmnisTab.DASHBOARD -> OctagonDashboard(
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
                }
            )
            OmnisTab.TEST_SEMANTIC -> {
                // Test Semantic tab fallthrough
            }
        }
    }
}
