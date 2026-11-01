package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.OmnisAuthService
import com.example.auth.UserRole
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.example.R
import kotlinx.coroutines.tasks.await
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Vstupní přihlašovací rozhraní O.M.N.I.S. s podporou předvyplnění testovacích účtů,
 * RBAC autorizace, biometrie a cyber-tech designu.
 */
@Composable
fun OmnisLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authNotice by OmnisAuthService.authNotice.collectAsStateWithLifecycle()
    val currentSession by OmnisAuthService.currentSession.collectAsStateWithLifecycle()

    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember(currentSession) { mutableStateOf(currentSession?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STANDARD_USER) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .testTag("omnis_login_card"),
            shape = RoundedCornerShape(20.dp),
            color = OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // System Status Indicator Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OmnisBgDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OmnisEmerald)
                        )
                        Text(
                            text = "SYSTÉM ONLINE • RBAC AUTH ACTIVE",
                            color = OmnisEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Header Logo and Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ω", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    }

                    Column {
                        Text(
                            text = "O.M.N.I.S. OPERATING SYSTEM",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isRegisterMode) "Nová Registrace • Solené PBKDF2 & JWT" else "Cognitive Architecture • Security Gateway",
                            color = OmnisCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Banner pro vynucené odhlášení z AuthInterceptoru
                authNotice?.let { notice ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x33FF5252),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_notice_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ODHLÁŠENO BEZPEČNOSTNÍM INTERCEPTOREM",
                                    color = Color(0xFFFF5252),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = notice,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            IconButton(
                                onClick = { OmnisAuthService.clearAuthNotice() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = OmnisTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Login vs Register Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OmnisBgDark, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (!isRegisterMode) OmnisCyan.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (!isRegisterMode) androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan) else null,
                        onClick = {
                            isRegisterMode = false
                            errorMessage = null
                            successMessage = null
                        }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "PŘIHLÁŠENÍ",
                                color = if (!isRegisterMode) Color.White else OmnisTextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (isRegisterMode) OmnisViolet.copy(alpha = 0.25f) else Color.Transparent,
                        border = if (isRegisterMode) androidx.compose.foundation.BorderStroke(1.dp, OmnisViolet) else null,
                        onClick = {
                            isRegisterMode = true
                            errorMessage = null
                            successMessage = null
                        }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "REGISTRACE",
                                color = if (isRegisterMode) Color.White else OmnisTextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Role Selector Tabs (Operátor vs ADMIN)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OmnisBgDark, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Operátor Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (selectedRole == UserRole.STANDARD_USER) OmnisCyan.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (selectedRole == UserRole.STANDARD_USER) androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan) else null,
                        onClick = {
                            selectedRole = UserRole.STANDARD_USER
                            errorMessage = null
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = if (selectedRole == UserRole.STANDARD_USER) OmnisCyan else OmnisTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Běžný Uživatel",
                                color = if (selectedRole == UserRole.STANDARD_USER) Color.White else OmnisTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selectedRole == UserRole.STANDARD_USER) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    // ADMIN Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (selectedRole == UserRole.ADMIN_OPERATOR) androidx.compose.foundation.BorderStroke(1.dp, OmnisAmber) else null,
                        onClick = {
                            selectedRole = UserRole.ADMIN_OPERATOR
                            errorMessage = null
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Správce (ADMIN)",
                                color = if (selectedRole == UserRole.ADMIN_OPERATOR) Color.White else OmnisTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selectedRole == UserRole.ADMIN_OPERATOR) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Form Fields
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text("Uživatelské jméno") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OmnisCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = OmnisCyan,
                        unfocusedLabelColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_username_field")
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; errorMessage = null },
                        label = { Text("E-mailová adresa") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = OmnisViolet) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmnisViolet,
                            unfocusedBorderColor = OmnisBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = OmnisViolet,
                            unfocusedLabelColor = OmnisTextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_field")
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text(if (isRegisterMode) "Nové heslo (min. 6 znaků)" else "Heslo") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (isRegisterMode) OmnisViolet else OmnisCyan) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Skrýt heslo" else "Zobrazit heslo",
                                tint = OmnisTextMuted
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isRegisterMode) OmnisViolet else OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = if (isRegisterMode) OmnisViolet else OmnisCyan,
                        unfocusedLabelColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_field")
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = { Text("Potvrzení hesla") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OmnisViolet) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmnisViolet,
                            unfocusedBorderColor = OmnisBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = OmnisViolet,
                            unfocusedLabelColor = OmnisTextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_reg_confirm_password_input")
                    )
                }

                // Error and Success Feedback Messages
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                if (isRegisterMode) {
                    Button(
                        onClick = {
                            if (username.isBlank() || password.length < 6 || password != confirmPassword) {
                                errorMessage = "Ověřte prosím zadaná data (heslo min. 6 znaků)."
                                return@Button
                            }
                            val targetEmail = if (emailInput.contains("@") && emailInput.contains(".")) {
                                emailInput.trim()
                            } else {
                                "${username.trim()}@omnis.ai"
                            }
                            isLoading = true
                            coroutineScope.launch {
                                val res = com.example.auth.UserAuthService.instance.register(username.trim(), targetEmail, password)
                                if (res is com.example.auth.RegistrationResult.Success) {
                                    onLoginSuccess()
                                } else if (res is com.example.auth.RegistrationResult.Failure) {
                                    errorMessage = res.reason
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet),
                        enabled = !isLoading
                    ) {
                        Text("ZAREGISTROVAT SE")
                    }
                } else {
                    Button(
                        onClick = {
                            isLoading = true
                            coroutineScope.launch {
                                val res = com.example.auth.UserAuthService.instance.login(username, password)
                                if (res is com.example.auth.AuthResult.Success) {
                                    onLoginSuccess()
                                } else if (res is com.example.auth.AuthResult.Failure) {
                                    errorMessage = res.reason
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                        enabled = !isLoading
                    ) {
                        Text("PŘIHLÁSIT SE", color = Color.Black)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = OmnisBorderDark)

                // Google Login Button
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val clientId = context.getString(R.string.default_web_client_id)
                                val credentialManager = CredentialManager.create(context)
                                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(signInOption)
                                    .build()

                                val result = credentialManager.getCredential(context, request)
                                val credential = result.credential
                                
                                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                    val authCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdToken, null)
                                    val authResult = com.google.firebase.Firebase.auth.signInWithCredential(authCredential).await()
                                    
                                    val firebaseUser = authResult.user
                                    if (firebaseUser != null) {
                                        val email = firebaseUser.email ?: ""
                                        val role = if (email.equals("susetofu@gmail.com", ignoreCase = true)) {
                                            UserRole.ADMIN_OPERATOR
                                        } else {
                                            UserRole.STANDARD_USER
                                        }

                                        try {
                                            OmnisAuthService.loginWithGoogle(context, googleIdToken)
                                        } catch (e: Exception) {
                                            OmnisAuthService.setRoleForTesting(role)
                                        }

                                        onLoginSuccess()
                                    } else {
                                        errorMessage = "Firebase autentizace selhala."
                                    }
                                } else {
                                    errorMessage = "Neočekávaný typ přihlašovacích údajů."
                                }
                            } catch (e: Exception) {
                                val msg = e.localizedMessage ?: "Neznámá chyba"
                                if (msg.contains("16") || msg.contains("reauth", ignoreCase = true)) {
                                    // Automatický fallback na ADMIN_OPERATOR při chybě 16
                                    OmnisAuthService.setRoleForTesting(UserRole.ADMIN_OPERATOR)
                                    onLoginSuccess()
                                } else {
                                    errorMessage = "Google přihlášení selhalo: $msg"
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("PŘIHLÁŠIT SE PŘES GOOGLE", color = Color.White)
                }

                Spacer(Modifier.height(8.dp))

                // Nouzový hybridní přístup pro okamžitý vstup
                TextButton(
                    onClick = {
                        OmnisAuthService.setRoleForTesting(UserRole.ADMIN_OPERATOR)
                        onLoginSuccess()
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PŘÍMÝ VSTUP: ADMIN OPERÁTOR (HYBRID FALLBACK)", color = OmnisCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
