package com.example.mocks

import com.example.api.ExecutionPlanStep
import com.example.api.HybridOmnisResponse
import com.example.api.ToolCallSpec
import com.example.data.OmnisRecord

/**
 * CentralMockDataRepository:
 * Centralizovaný repozitář testovacích dat pro elimining duplicity v testech (Dekarbonizace Codebase, Fáze 3).
 * Sjednocuje běžně používané mock odpovědi, záznamy a uživatelské profily na jednom místě.
 */
object CentralMockDataRepository {

    val MOCK_HYBRID_RESPONSE = HybridOmnisResponse(
        intent = "systems_architecture",
        confidenceScore = 0.95f,
        executionPlan = listOf(
            ExecutionPlanStep(
                stepNumber = 1,
                actionDescription = "Dekonstrukce systémových parametrů",
                toolCall = ToolCallSpec(
                    toolId = "sys_analyzer",
                    parameters = mapOf("mode" to "strict")
                )
            ),
            ExecutionPlanStep(
                stepNumber = 2,
                actionDescription = "Korelace 8D tenzorů",
                toolCall = ToolCallSpec(
                    toolId = "tensor_engine",
                    parameters = mapOf("dimensions" to 8)
                )
            )
        ),
        immediateResponse = "Strukturovaná odpověď byla úspěšně syntetizována.",
        requiredOutputFormat = "markdown"
    )

    fun createMockRecord(
        role: String = "assistant",
        content: String = "Testovací syntetická odpověď systému OMNIS.",
        compositeScore: Float = 0.95f
    ): OmnisRecord {
        return OmnisRecord(
            role = role,
            content = content,
            cognitiveProcess = "Sémantická dekonstrukce -> AI Act Vodoznak -> Result",
            followUpQuestions = "Jaký je další krok?|Chcete provést export?",
            valSys = 0.95f,
            valEcon = 0.90f,
            valPsych = 0.85f,
            valEco = 0.90f,
            valLaw = 1.0f,
            valSec = 0.95f,
            valPhys = 0.90f,
            valSoc = 0.85f,
            compositeScore = compositeScore,
            timestamp = System.currentTimeMillis()
        )
    }

    val MOCK_OIDC_TOKEN = "oidc_jwt_access_mock_token_12345"
    val MOCK_ADMIN_USER = "admin.cso@omnis.cloud"
    val MOCK_OPERATOR_USER = "operator.dev@omnis.cloud"
}
