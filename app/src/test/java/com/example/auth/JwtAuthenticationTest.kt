package com.example.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.crypto.PasswordHasher
import com.example.auth.jwt.JwtAuthInterceptor
import com.example.auth.jwt.JwtTokenManager
import com.example.data.OmnisDatabase
import com.example.data.UserAuthDao
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JwtAuthenticationTest {

    private lateinit var context: Context
    private lateinit var database: OmnisDatabase
    private lateinit var userAuthDao: UserAuthDao
    private lateinit var userAuthService: UserAuthService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = OmnisDatabase.getInMemoryDatabase(context)
        userAuthDao = database.userAuthDao()
        userAuthService = UserAuthService { userAuthDao }
        OmnisAuthService.logout(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // 1. PBKDF2 PASSWORD HASHER TESTS
    // =========================================================================

    @Test
    fun testPasswordHasher_generatesUniqueSaltAndValidHash() {
        val password = "SuperSecretPassword123!"
        val result1 = PasswordHasher.hashPassword(password)
        val result2 = PasswordHasher.hashPassword(password)

        // Každé hashování musí mít unikátní náhodnou sůl
        assertNotEquals(result1.saltHex, result2.saltHex)
        assertNotEquals(result1.hashHex, result2.hashHex)
        assertEquals(PasswordHasher.ITERATIONS, result1.iterations)

        // Správné ověření hesla vůči odpovídající soli
        assertTrue(PasswordHasher.verifyPassword(password, result1.hashHex, result1.saltHex))
        assertTrue(PasswordHasher.verifyPassword(password, result2.hashHex, result2.saltHex))

        // Neplatné heslo musí selhat
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", result1.hashHex, result1.saltHex))
    }

    @Test
    fun testPasswordHasher_hexConversionIsBidirectional() {
        val testBytes = byteArrayOf(0x00, 0x12, 0x7F, -0x80, -0x01, 0x42)
        val hex = PasswordHasher.bytesToHex(testBytes)
        val restoredBytes = PasswordHasher.hexToBytes(hex)
        assertArrayEquals(testBytes, restoredBytes)
    }

    // =========================================================================
    // 2. RFC 7519 JWT TOKEN MANAGER TESTS
    // =========================================================================

    @Test
    fun testJwtTokenManager_createsValidTokenAndExtractsClaims() {
        val username = "chief_operator"
        val role = UserRole.ADMIN_OPERATOR.name
        val token = JwtTokenManager.createToken(
            subject = username,
            role = role,
            durationSeconds = 3600
        )

        val parts = token.split(".")
        assertEquals(3, parts.size)

        when (val validation = JwtTokenManager.validateToken(token)) {
            is JwtTokenManager.JwtValidationResult.Valid -> {
                assertEquals(username, validation.claims.subject)
                assertEquals(role, validation.claims.role)
                assertTrue(validation.claims.expiresAt > validation.claims.issuedAt)
                assertTrue(validation.claims.jwtId.isNotBlank())
            }
            else -> fail("Token validation should have succeeded, but got $validation")
        }
    }

    @Test
    fun testJwtTokenManager_rejectsTamperedPayload() {
        val token = JwtTokenManager.createToken("operator_1", UserRole.STANDARD_USER.name)
        val parts = token.split(".")

        // Podvržení payloadu (nahrazení části za jiný Base64 text)
        val tamperedPayload = JwtTokenManager.base64UrlEncode("{\"sub\":\"hacker\",\"role\":\"ADMIN_OPERATOR\"}".toByteArray())
        val tamperedToken = "${parts[0]}.$tamperedPayload.${parts[2]}"

        val validation = JwtTokenManager.validateToken(tamperedToken)
        assertTrue(validation is JwtTokenManager.JwtValidationResult.InvalidSignature)
    }

    @Test
    fun testJwtTokenManager_detectsExpiredToken() {
        // Vytvoříme token s nulovou trvanlivostí (-10 sekund do minulosti)
        val token = JwtTokenManager.createToken(
            subject = "expired_user",
            role = "STANDARD_USER",
            durationSeconds = -10
        )

        val validation = JwtTokenManager.validateToken(token)
        assertTrue(validation is JwtTokenManager.JwtValidationResult.Expired)
    }

    // =========================================================================
    // 3. USER REGISTRATION & DATABASE-BACKED AUTHENTICATION TESTS
    // =========================================================================

    @Test
    fun testUserAuthService_registrationStoresHashedPasswordAndIssuesJwt() = runBlocking {
        val username = "new_engineer"
        val password = "StrongPassword@2026"
        val role = UserRole.ADMIN_OPERATOR

        val regResult = userAuthService.register(context, username, password, role)
        assertTrue(regResult is RegistrationResult.Success)

        val success = regResult as RegistrationResult.Success
        assertEquals(username, success.user.username)
        assertEquals(role.name, success.user.role)

        // Heslo v databázi nesmí být čistý text!
        assertNotEquals(password, success.user.passwordHash)
        assertTrue(success.user.passwordSalt.isNotBlank())

        // Vydaný token je platný podepsaný JWT
        val tokenValidation = JwtTokenManager.validateToken(success.token)
        assertTrue(tokenValidation is JwtTokenManager.JwtValidationResult.Valid)
        val claims = (tokenValidation as JwtTokenManager.JwtValidationResult.Valid).claims
        assertEquals(username, claims.subject)
        assertEquals(role.name, claims.role)

        // Pokus o duplicitní registraci stejného uživatele musí selhat
        val duplicateResult = userAuthService.register(context, username, password, role)
        assertTrue(duplicateResult is RegistrationResult.Failure)
    }

    @Test
    fun testUserAuthService_rejectsShortPasswordsAndUsernames() = runBlocking {
        val shortUserResult = userAuthService.register(context, "ab", "validPassword123")
        assertTrue(shortUserResult is RegistrationResult.Failure)

        val shortPassResult = userAuthService.register(context, "validUser", "12345")
        assertTrue(shortPassResult is RegistrationResult.Failure)
    }

    @Test
    fun testUserAuthService_loginVerifiesPasswordAndGeneratesJwt() = runBlocking {
        val username = "security_lead"
        val password = "DefensePassword2026!"
        val role = UserRole.ADMIN_OPERATOR

        userAuthService.register(context, username, password, role)

        // 1. Úspěšné přihlášení
        val loginSuccess = userAuthService.login(context, username, password, role)
        assertTrue(loginSuccess is AuthResult.Success)
        val session = (loginSuccess as AuthResult.Success).session
        assertEquals(username, session.username)
        assertEquals(role, session.role)

        val tokenValidation = JwtTokenManager.validateToken(session.token)
        assertTrue(tokenValidation is JwtTokenManager.JwtValidationResult.Valid)

        // 2. Chybné heslo
        val loginWrongPass = userAuthService.login(context, username, "WrongPassword!", role)
        assertTrue(loginWrongPass is AuthResult.Failure)

        // 3. Neexistující uživatel
        val loginUnknown = userAuthService.login(context, "non_existent_user", password, role)
        assertTrue(loginUnknown is AuthResult.Failure)
    }

    @Test
    fun testUserAuthService_bootstrapDefaultUsers() = runBlocking {
        // Přihlášení výchozího účtu 'admin'
        val loginAdmin = userAuthService.login(context, "admin", "Omnis@Admin2025", UserRole.ADMIN_OPERATOR)
        assertTrue(loginAdmin is AuthResult.Success)

        // Ověříme, že uživatel byl zároveň na persistován do Room s PBKDF2 hashem
        val entity = userAuthDao.findByUsername("admin")
        assertNotNull(entity)
        assertEquals("admin", entity?.username)
        assertNotEquals("Omnis@Admin2025", entity?.passwordHash)
    }

    // =========================================================================
    // 4. OKHTTP JWT AUTH INTERCEPTOR MIDDLEWARE TESTS
    // =========================================================================

    private class FakeInterceptorChain(
        private val initialRequest: Request
    ) : Interceptor.Chain {
        var proceedRequest: Request? = null

        override fun request(): Request = initialRequest

        override fun proceed(request: Request): Response {
            proceedRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{\"status\":\"success\"}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 1000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 1000
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 1000
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    @Test
    fun testJwtAuthInterceptor_injectsBearerHeader() {
        val sampleJwt = JwtTokenManager.createToken("test_operator", UserRole.STANDARD_USER.name)

        val interceptor = JwtAuthInterceptor(
            tokenProvider = { sampleJwt },
            enforceStrictRouteProtection = false
        )

        val request = Request.Builder()
            .url("https://omnis.system/api/v1/omnis/query")
            .build()

        val chain = FakeInterceptorChain(request)
        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertNotNull(chain.proceedRequest)
        val authHeader = chain.proceedRequest?.header("Authorization")
        assertNotNull(authHeader)
        assertEquals("Bearer $sampleJwt", authHeader)
    }

    @Test
    fun testJwtAuthInterceptor_enforcesRouteProtectionWithoutToken() {
        val interceptor = JwtAuthInterceptor(
            tokenProvider = { null }, // Žádný token
            enforceStrictRouteProtection = true
        )

        val request = Request.Builder()
            .url("https://omnis.system/api/v1/omnis/protected_resource")
            .build()

        val chain = FakeInterceptorChain(request)
        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        assertNull(chain.proceedRequest) // Řetězec nebyl spuštěn dál
    }

    @Test
    fun testJwtAuthInterceptor_enforcesAdminRoutePermission() {
        val standardUserJwt = JwtTokenManager.createToken("standard_user", UserRole.STANDARD_USER.name)

        val interceptor = JwtAuthInterceptor(
            tokenProvider = { standardUserJwt },
            enforceStrictRouteProtection = true
        )

        val request = Request.Builder()
            .url("https://omnis.system/api/admin/system/override")
            .build()

        val chain = FakeInterceptorChain(request)
        val response = interceptor.intercept(chain)

        assertEquals(403, response.code)
        assertNull(chain.proceedRequest) // Řetězec byl zablokován
    }

    @Test
    fun testJwtAuthInterceptor_emitsAuthEventBusOn401Unauthorized() {
        var eventReceived: com.example.auth.jwt.AuthEvent? = null
        val testJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).launch {
            com.example.auth.jwt.AuthEventBus.events.collect { event ->
                eventReceived = event
            }
        }

        val interceptor = JwtAuthInterceptor(
            tokenProvider = { null },
            enforceStrictRouteProtection = true
        )

        val request = Request.Builder()
            .url("https://omnis.system/api/v1/omnis/test_endpoint")
            .build()

        val chain = FakeInterceptorChain(request)
        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        assertNotNull(eventReceived)
        assertTrue(eventReceived is com.example.auth.jwt.AuthEvent.Unauthorized)
        assertEquals(401, (eventReceived as com.example.auth.jwt.AuthEvent.Unauthorized).statusCode)
        testJob.cancel()
    }

    @Test
    fun testDatabasePassphraseProvider_generatesAndCachesValid256BitKey() {
        val passphrase1 = com.example.security.DatabasePassphraseProvider.getDatabasePassphrase(context)
        assertNotNull(passphrase1)
        assertTrue(passphrase1.size >= 32)

        val passphrase2 = com.example.security.DatabasePassphraseProvider.getDatabasePassphrase(context)
        assertArrayEquals(passphrase1, passphrase2)
    }
}
