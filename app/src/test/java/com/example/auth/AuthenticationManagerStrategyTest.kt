package com.example.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthenticationManagerStrategyTest {

    private lateinit var context: Context
    private lateinit var authManager: AuthenticationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authManager = AuthenticationManager.instance
        authManager.logout(context)
    }

    @Test
    fun testAdminStrategy_fullPermissionsAndCredentialVerification() {
        val strategy = UserAuthorizationStrategyRegistry.getStrategy(UserRole.ADMIN_OPERATOR)

        assertEquals(UserRole.ADMIN_OPERATOR, strategy.role)
        assertTrue(strategy.canAccessSystemActions())
        assertTrue(strategy.canAccessDevDiagnostic())
        assertTrue(strategy.canModifySystemState())
        assertTrue(strategy.hasPermission(SystemPermission.ACCESS_SYSTEM_ACTIONS))
        assertTrue(strategy.hasPermission(SystemPermission.EXECUTE_DATA_SYNC))
        assertTrue(strategy.hasPermission(SystemPermission.MODIFY_SYSTEM_STATE))

        // Ověření platných operátorských klíčů
        assertTrue(strategy.validateCredentials("omnis2026", authManager::hashSecret))
        assertTrue(strategy.validateCredentials("root_operator", authManager::hashSecret))
        assertTrue(strategy.validateCredentials("admin", authManager::hashSecret))

        // Neplatné heslo
        assertFalse(strategy.validateCredentials("wrong_password", authManager::hashSecret))
        assertFalse(strategy.validateCredentials("", authManager::hashSecret))
    }

    @Test
    fun testStandardUserStrategy_restrictedPermissions() {
        val strategy = UserAuthorizationStrategyRegistry.getStrategy(UserRole.STANDARD_USER)

        assertEquals(UserRole.STANDARD_USER, strategy.role)
        assertFalse(strategy.canAccessSystemActions())
        assertFalse(strategy.canAccessDevDiagnostic())
        assertFalse(strategy.canModifySystemState())

        // Povolené standardní čtecí a chatové oprávnění
        assertTrue(strategy.hasPermission(SystemPermission.ACCESS_KNOWLEDGE_BASE))
        assertTrue(strategy.hasPermission(SystemPermission.ACCESS_TELEMETRY))
        assertFalse(strategy.hasPermission(SystemPermission.MODIFY_SYSTEM_STATE))
        assertFalse(strategy.hasPermission(SystemPermission.EXECUTE_DATA_SYNC))

        // Běžný uživatel nevyžaduje tajný klíč
        assertTrue(strategy.validateCredentials("", authManager::hashSecret))
    }

    @Test
    fun testAuditorStrategy_governanceOnlyPermissions() {
        val strategy = AuditorAuthorizationStrategy()

        assertEquals(UserRole.STANDARD_USER, strategy.role)
        assertFalse(strategy.canAccessSystemActions())
        assertTrue(strategy.canAccessDevDiagnostic())
        assertFalse(strategy.canModifySystemState())
        assertTrue(strategy.hasPermission(SystemPermission.AUDIT_GOVERNANCE))
        assertFalse(strategy.hasPermission(SystemPermission.MODIFY_SYSTEM_STATE))
    }

    @Test
    fun testAuthenticationManager_loginSuccessAndSessionPersistence() {
        val result = authManager.login(context, "operator1", "omnis2026", UserRole.ADMIN_OPERATOR)
        assertTrue(result is AuthResult.Success)

        val success = result as AuthResult.Success
        assertEquals("operator1", success.session.username)
        assertEquals(UserRole.ADMIN_OPERATOR, success.session.role)
        assertTrue(success.session.token.startsWith("omnis_jwt_"))

        // Obnova session z úložiště
        val restored = authManager.restoreSession(context)
        assertNotNull(restored)
        assertEquals("operator1", restored!!.username)
        assertEquals(UserRole.ADMIN_OPERATOR, restored.role)
    }

    @Test
    fun testAuthenticationManager_loginFailureOnInvalidSecret() {
        val result = authManager.login(context, "operator1", "bad_pass", UserRole.ADMIN_OPERATOR)
        assertTrue(result is AuthResult.Failure)
        val failure = result as AuthResult.Failure
        assertTrue(failure.reason.contains("Neplatné přístupové oprávnění"))
    }

    @Test
    fun testAuthenticationManager_blankUsernameValidation() {
        val result = authManager.login(context, "   ", "omnis2026", UserRole.ADMIN_OPERATOR)
        assertTrue(result is AuthResult.Failure)
        val failure = result as AuthResult.Failure
        assertTrue(failure.reason.contains("Uživatelské jméno nesmí být prázdné"))
    }

    @Test
    fun testOmnisAuthService_backwardCompatibilityIntegration() {
        OmnisAuthService.logout(context)
        assertFalse(OmnisAuthService.isAuthenticated.value)

        val loggedIn = OmnisAuthService.login(context, "alice", "", UserRole.STANDARD_USER)
        assertTrue(loggedIn)
        assertTrue(OmnisAuthService.isAuthenticated.value)
        assertEquals(UserRole.STANDARD_USER, OmnisAuthService.currentUserRole.value)
    }
}
