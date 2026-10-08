package com.example.action

import com.example.auth.OmnisAuthService
import com.example.auth.UserRole
import com.example.data.OmnisDao
import com.example.transaction.DataTransactionController
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Strukturovaný akční payload (Action-Driven Architecture)
 */
data class ActionPayload(
    val intent: String,
    val actionId: String,
    val parameters: Map<String, Any> = emptyMap()
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("intent", intent)
        json.put("action_id", actionId)
        val paramsJson = JSONObject()
        parameters.forEach { (k, v) -> paramsJson.put(k, v) }
        json.put("parameters", paramsJson)
        return json.toString(2)
    }
}

/**
 * Výsledek exekuce akčního dispečeru
 */
data class ActionExecutionResult(
    val actionId: String,
    val isSuccess: Boolean,
    val statusCode: Int,
    val logs: List<String>,
    val outputData: Map<String, Any> = emptyMap(),
    val summaryReport: String
)

/**
 * Backend Dispatcher:
 * Odlehčený delegátor pro odbavení systémových akcí.
 * Veškerá transakční logika a RBAC řízení byla dekomponována do DataTransactionController
 * s využitím návrhového vzoru Strategie (Strategy Pattern) a techniky Extract Method.
 * Cyklomatická složitost: 1.
 */
object OmnisActionDispatcher {

    private val controller: DataTransactionController = DataTransactionController.instance

    /**
     * Spustí deterministickou nativní akci podle ID a parametrů skrze polymorfní transakční kontroler.
     * Cyklomatická složitost: 1.
     */
    suspend fun executeAction(
        payload: ActionPayload,
        omnisDao: OmnisDao? = null,
        callerRole: UserRole = OmnisAuthService.currentUserRole.value
    ): ActionExecutionResult = withContext(com.example.api.OmnisGeminiClient.ioDispatcher) {
        controller.processTransaction(payload, omnisDao, callerRole)
    }
}
