package com.example.transaction

import com.example.action.ActionPayload
import com.example.action.OmnisActionDispatcher
import com.example.auth.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataTransactionControllerStrategyTest {

    private val controller = DataTransactionController.instance

    @Test
    fun testAutoSyncStrategy_authorizedAdminExecution() = runBlocking {
        val payload = ActionPayload(
            intent = "system_maintenance",
            actionId = "postgres_auto_sync",
            parameters = mapOf("engine" to "PostgreSQL Cloud SQL")
        )

        val result = controller.processTransaction(
            payload = payload,
            dao = null,
            callerRole = UserRole.ADMIN_OPERATOR
        )

        assertTrue(result.isSuccess)
        assertEquals(200, result.statusCode)
        assertEquals("postgres_auto_sync", result.actionId)
        assertTrue(result.summaryReport.contains("Synchronizace"))
    }

    @Test
    fun testAutoSyncStrategy_deniedForStandardUser() = runBlocking {
        val payload = ActionPayload(
            intent = "system_maintenance",
            actionId = "postgres_auto_sync"
        )

        val result = controller.processTransaction(
            payload = payload,
            dao = null,
            callerRole = UserRole.STANDARD_USER
        )

        assertFalse(result.isSuccess)
        assertEquals(403, result.statusCode)
        assertTrue(result.summaryReport.contains("Přístup zamítnut"))
    }

    @Test
    fun testDbConnectivityTest_allowedForBothRoles() = runBlocking {
        val payload = ActionPayload(
            intent = "diagnostics",
            actionId = "db_connectivity_test"
        )

        val adminResult = controller.processTransaction(payload, null, UserRole.ADMIN_OPERATOR)
        val userResult = controller.processTransaction(payload, null, UserRole.STANDARD_USER)

        // Both roles are permitted to run basic connectivity tests
        assertEquals(500, adminResult.statusCode) // null dao yields 500 cleanly without exception
        assertEquals(500, userResult.statusCode)
        assertFalse(adminResult.summaryReport.contains("Přístup zamítnut"))
        assertFalse(userResult.summaryReport.contains("Přístup zamítnut"))
    }

    @Test
    fun testUnknownAction_returns404Cleanly() = runBlocking {
        val payload = ActionPayload(
            intent = "unknown",
            actionId = "non_existent_action_xyz"
        )

        val result = controller.processTransaction(payload, null, UserRole.ADMIN_OPERATOR)
        assertFalse(result.isSuccess)
        assertEquals(404, result.statusCode)
        assertTrue(result.summaryReport.contains("nebyla nalezena"))
    }

    @Test
    fun testOmnisActionDispatcher_delegatesToControllerSeamlessly() = runBlocking {
        val payload = ActionPayload(
            intent = "governance",
            actionId = "ai_governance_export"
        )

        val result = OmnisActionDispatcher.executeAction(payload, null, UserRole.ADMIN_OPERATOR)
        assertTrue(result.isSuccess)
        assertEquals(200, result.statusCode)
        assertEquals("ai_governance_export", result.actionId)
        assertTrue(result.summaryReport.contains("AI Governance"))
    }
}
