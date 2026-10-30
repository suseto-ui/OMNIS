package com.example.transaction

import com.example.action.ActionExecutionResult
import com.example.action.ActionPayload
import com.example.auth.OmnisAuthService
import com.example.auth.UserAuthorizationStrategyRegistry
import com.example.auth.UserRole
import com.example.data.OmnisDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DataTransactionController:
 * Centrální transakční kontroler nahrazující původní monolitické if-else / switch struktury.
 * Využívá vzor Strategie (Strategy Pattern) a techniku 'Extract Method'.
 * Každá metoda má cyklomatickou složitost <= 3.
 */
class DataTransactionController(
    private val strategies: Map<String, DataTransactionStrategy> = defaultStrategies()
) {
    companion object {
        val instance: DataTransactionController by lazy { DataTransactionController() }

        private fun defaultStrategies(): Map<String, DataTransactionStrategy> {
            val list: List<DataTransactionStrategy> = listOf(
                PostgresAutoSyncTransactionStrategy(),
                DbConnectivityTransactionStrategy(),
                ResilienceAuditTransactionStrategy(),
                GovernanceAuditTransactionStrategy(),
                SelfHealingTransactionStrategy(),
                CrossThreadSearchTransactionStrategy(),
                ExtractTasksTransactionStrategy(),
                LeverageInterventionTransactionStrategy(),
                IotActuatorExecutionTransactionStrategy()
            )
            return list.associateBy { it.actionId }
        }
    }

    /**
     * Zpracuje a odbaví příchozí datovou transakci s ověřením oprávnění.
     * Cyklomatická složitost: 2
     */
    suspend fun processTransaction(
        payload: ActionPayload,
        dao: OmnisDao? = null,
        callerRole: UserRole = OmnisAuthService.currentUserRole.value
    ): ActionExecutionResult = withContext(Dispatchers.IO) {
        val context = buildTransactionContext(payload, dao)
        val strategy = strategies[payload.actionId]

        if (strategy == null) {
            return@withContext handleUnknownAction(payload, context)
        }

        if (!isAuthorizedForAction(callerRole, strategy)) {
            return@withContext handleUnauthorizedAction(payload, context, callerRole)
        }

        return@withContext executeStrategySafely(strategy, context)
    }

    // =========================================================================
    // EXTRACT METHOD: Izolované privátní metody s CC = 1
    // =========================================================================

    private fun buildTransactionContext(payload: ActionPayload, dao: OmnisDao?): TransactionContext {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val logs = mutableListOf<String>()
        logs.add("[$timeStamp] TRANSACTION-CONTROLLER: Odchycen intent '${payload.intent}' -> action_id: '${payload.actionId}'")
        return TransactionContext(payload = payload, omnisDao = dao, timeStamp = timeStamp, logs = logs)
    }

    private fun isAuthorizedForAction(role: UserRole, strategy: DataTransactionStrategy): Boolean {
        val authStrategy = UserAuthorizationStrategyRegistry.getStrategy(role)
        return authStrategy.hasPermission(strategy.requiredPermission) &&
                authStrategy.canExecuteTransaction(strategy.actionId)
    }

    private suspend fun executeStrategySafely(
        strategy: DataTransactionStrategy,
        context: TransactionContext
    ): ActionExecutionResult {
        return try {
            strategy.execute(context)
        } catch (e: Exception) {
            context.logs.add("[${context.timeStamp}] ERROR: Selhání exekuce strategie '${strategy.actionId}': ${e.message}")
            ActionExecutionResult(
                actionId = strategy.actionId,
                isSuccess = false,
                statusCode = 500,
                logs = context.logs,
                outputData = mapOf("error" to (e.message ?: "Unknown error")),
                summaryReport = "Transakce ${strategy.actionId} selhala s výjimkou: ${e.localizedMessage}"
            )
        }
    }

    private fun handleUnknownAction(payload: ActionPayload, context: TransactionContext): ActionExecutionResult {
        context.logs.add("[${context.timeStamp}] ERROR: Neznámý action_id '${payload.actionId}'.")
        return ActionExecutionResult(
            actionId = payload.actionId,
            isSuccess = false,
            statusCode = 404,
            logs = context.logs,
            outputData = emptyMap(),
            summaryReport = "Akce '${payload.actionId}' nebyla nalezena v registru strategií."
        )
    }

    private fun handleUnauthorizedAction(
        payload: ActionPayload,
        context: TransactionContext,
        role: UserRole
    ): ActionExecutionResult {
        context.logs.add("[${context.timeStamp}] RBAC-DENIED: Uživatel s rolí '${role.roleName}' nemá oprávnění spustit '${payload.actionId}'.")
        return ActionExecutionResult(
            actionId = payload.actionId,
            isSuccess = false,
            statusCode = 403,
            logs = context.logs,
            outputData = mapOf("denied_role" to role.name),
            summaryReport = "Přístup zamítnut: Vaše uživatelská role (${role.roleName}) nemá dostatečná oprávnění."
        )
    }
}
