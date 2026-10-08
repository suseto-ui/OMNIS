package com.example.transaction

import com.example.action.ActionExecutionResult
import com.example.action.ActionPayload
import com.example.auth.SystemPermission
import com.example.data.OmnisDao

/**
 * Kontext datové transakce
 */
data class TransactionContext(
    val payload: ActionPayload,
    val omnisDao: OmnisDao?,
    val timeStamp: String,
    val logs: MutableList<String>
)

/**
 * Behaviorální vzor Strategie pro zpracování datových transakcí (DataTransactionStrategy).
 * Každá transakční operace je zapouzdřena do vlastní třídy s CC = 1 až 2.
 */
interface DataTransactionStrategy {
    val actionId: String
    val requiredPermission: SystemPermission

    suspend fun execute(context: TransactionContext): ActionExecutionResult
}
