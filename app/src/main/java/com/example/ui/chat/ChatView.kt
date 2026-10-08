package com.example.ui.chat

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.action.ActionExecutionResult
import com.example.action.ActionPayload
import com.example.auth.UserRole
import com.example.data.MemoryFragment
import com.example.data.OmnisRecord
import com.example.ui.ActionDrivenInteractivePanel
import com.example.ui.OmnisViewModel
import com.example.ui.UserExperienceMode
import com.example.ui.beginner.BeginnerChatHeader
import com.example.ui.beginner.BeginnerGuideDialog
import com.example.ui.theme.OmnisBgDark

/**
 * Hlavní konverzační a analytické rozhraní O.M.N.I.S. (ChatView).
 * Zahrnuje proudové zprávy, 8D syntézu, sémantickou bránu a interaktivní režim pro začátečníky.
 */
@Composable
fun ChatView(
    records: List<OmnisRecord>,
    isLoading: Boolean,
    isOcrLoading: Boolean,
    streamState: OmnisViewModel.StreamState?,
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
    onExecuteActionPayload: (ActionPayload) -> Unit = {},
    lastActionResult: ActionExecutionResult? = null,
    isActionExecuting: Boolean = false,
    userRole: UserRole = UserRole.STANDARD_USER,
    activeMemoryFragments: List<MemoryFragment> = emptyList(),
    isPromptGuideEnabled: Boolean = false,
    onDisablePromptGuide: () -> Unit = {},
    isCircuitBreakerTripped: Boolean = false,
    onToggleCircuitBreaker: () -> Unit = {},
    isPromptGatewayEnabled: Boolean = false,
    onTogglePromptGateway: () -> Unit = {},
    currentDomain: String = "All",
    onDomainChange: (String) -> Unit = {},
    userExperienceMode: UserExperienceMode = UserExperienceMode.STANDARD,
    onToggleUserMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showBeginnerGuideDialog by remember { mutableStateOf(false) }

    if (showBeginnerGuideDialog) {
        BeginnerGuideDialog(
            onDismiss = { showBeginnerGuideDialog = false }
        )
    }

    // Řazení zpráv pro reverseLayout: v obráceném rozvržení je index 0 dole (nejnovější zpráva u vstupního pole),
    // takže zprávy řadíme sestupně podle času. Tím se historie vykreslí v přirozeném sledu shora dolů (nejstarší nahoře -> nejnovější dole).
    val displayRecords = remember(records) { records.sortedByDescending { it.timestamp } }

    // Automatický scroll na požadované ID nebo nejnovější zprávu
    LaunchedEffect(scrollToId) {
        if (scrollToId != null) {
            val index = displayRecords.indexOfFirst { it.id == scrollToId }
            if (index >= 0) {
                listState.animateScrollToItem(if (streamState != null && streamState.text.isNotBlank()) index + 1 else index)
                onScrollComplete()
            }
        }
    }

    LaunchedEffect(records.size, streamState?.text?.length) {
        if ((records.isNotEmpty() || (streamState != null && streamState.text.isNotBlank())) && scrollToId == null) {
            listState.animateScrollToItem(0)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    val isImeVisible = WindowInsets.isImeVisible

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
    ) {
        // Stavový informační pruh Gemini kaskády (skryje se při psaní na klávesnici)
        AnimatedVisibility(
            visible = !isImeVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ChatStatusBanner(
                userRole = userRole,
                userExperienceMode = userExperienceMode
            )
        }

        // Začátečnické záhlaví s nápovědou (zobrazuje se v režimu STANDARD, skryje se při otevřené klávesnici)
        AnimatedVisibility(
            visible = userExperienceMode == UserExperienceMode.STANDARD && !isImeVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            BeginnerChatHeader(
                onQuickPromptSelected = onQuickQuery,
                onOpenGuide = { showBeginnerGuideDialog = true },
                onSwitchToExpert = onToggleUserMode,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Seznam zpráv
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Streaming stav (připojen na samý konec konverzace u vstupního pole - index 0)
                if (streamState != null && streamState.text.isNotBlank()) {
                    item(key = "streaming_response") {
                        ChatMessageItem(
                            record = OmnisRecord(
                                id = -1,
                                role = "assistant",
                                content = streamState.text,
                                cognitiveProcess = streamState.stage,
                                domain = currentDomain,
                                timestamp = System.currentTimeMillis()
                            ),
                            onQuickQuery = onQuickQuery,
                            onSpeak = onSpeak,
                            onExportPdf = onExportPdf,
                            selectedDomains = selectedDomains,
                            onDomainClick = onDomainClick,
                            onDomainLongClick = onDomainLongClick,
                            onAuthorize = {},
                            userExperienceMode = userExperienceMode
                        )
                    }
                }

                items(
                    items = displayRecords,
                    key = { it.id }
                ) { record ->
                    ChatMessageItem(
                        record = record,
                        onQuickQuery = onQuickQuery,
                        onSpeak = onSpeak,
                        onExportPdf = onExportPdf,
                        selectedDomains = selectedDomains,
                        onDomainClick = onDomainClick,
                        onDomainLongClick = onDomainLongClick,
                        onAuthorize = { onAuthorizeRecord(record) },
                        userExperienceMode = userExperienceMode
                    )
                }
            }
        }

        // Vstupní lišta zpráv
        ChatInputBar(
            inputQuery = inputQuery,
            onQueryChange = onQueryChange,
            onSend = onSend,
            onAttachFile = { /* Akce připojení souboru */ },
            onCameraClick = { /* Akce kamery */ },
            onMicClick = { /* Akce mikrofonu */ },
            isLoading = isLoading,
            isOcrLoading = isOcrLoading,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!isImeVisible) Modifier.navigationBarsPadding() else Modifier)
        )
    }
}
