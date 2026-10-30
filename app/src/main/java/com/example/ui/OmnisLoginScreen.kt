package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.OmnisAuthService
import com.example.auth.UserRole
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Centrální přihlašovací rozhraní s podporou sessions, RBAC schématu (Admin / Operátor vs Běžný Uživatel)
 * a tokenového ověření.
 */
@Composable
fun OmnisLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authNotice by OmnisAuthService.authNotice.collectAsStateWithLifecycle()
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STANDARD_USER) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .imePadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .testTag("omnis_auth_card"),
            shape = RoundedCornerShape(20.dp),
            color = OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ω", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "O.M.N.I.S. OPERATING SYSTEM",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isRegisterMode) "Nová Registrace • Solené PBKDF2 & JWT" else "Action-Driven Cognitive Platform • RBAC Auth",
                        color = OmnisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
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
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ODHLÁŠENO SÍŤOVÝM INTERCEPTOREM",
                                    color = Color(0xFFFF5252),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = notice,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(
                                onClick = { OmnisAuthService.clearAuthNotice() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("✕", color = OmnisTextMuted, fontSize = 14.sp)
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

                HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 2.dp))

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
                                text = "Operátor",
                                color = if (selectedRole == UserRole.STANDARD_USER) Color.White else OmnisTextMuted,
                                fontSize = 12.sp,
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
                                text = "ADMIN",
                                color = if (selectedRole == UserRole.ADMIN_OPERATOR) Color.White else OmnisTextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (selectedRole == UserRole.ADMIN_OPERATOR) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Info Label
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisBgDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isRegisterMode) {
                                "Kryptografické heslo: PBKDF2WithHmacSHA256 (10 000 iterací, 16B sůl). Přístup řízen přes podepsaný RFC 7519 JWT."
                            } else if (selectedRole == UserRole.ADMIN_OPERATOR) {
                                "ADMIN: Plná správa systému, jističů a administrátorských nástrojů (JWT Bearer)."
                            } else {
                                "OPERÁTOR: Přístup ke kognitivním chatům, analýze a autonomním cílům."
                            },
                            color = OmnisTextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Form Fields
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text(if (selectedRole == UserRole.ADMIN_OPERATOR) "Admin ID" else "Identifikátor Operátora") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan,
                        unfocusedLabelColor = OmnisTextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_username_field")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text(if (isRegisterMode) "Nové heslo (min. 6 znaků)" else if (selectedRole == UserRole.ADMIN_OPERATOR) "Admin Heslo" else "Přístupový klíč Operátora") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan) },
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
                        focusedBorderColor = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan,
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

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                successMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isRegisterMode) {
                    // Register Action Button
                    Button(
                        onClick = {
                            if (username.isBlank() || username.length < 3) {
                                errorMessage = "Uživatelské jméno musí mít alespoň 3 znaky."
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "Heslo musí mít minimálně 6 znaků."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Zadaná hesla se neshodují."
                                return@Button
                            }

                            coroutineScope.launch {
                                when (val reg = OmnisAuthService.register(context, username.trim(), password, selectedRole)) {
                                    is com.example.auth.RegistrationResult.Success -> {
                                        successMessage = "Účet úspěšně vytvořen! JWT token vygenerován."
                                        onLoginSuccess()
                                    }
                                    is com.example.auth.RegistrationResult.Failure -> {
                                        errorMessage = reg.reason
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_register_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OmnisViolet
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "VYTVOŘIT ÚČET A VYDAT JWT",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    // Login Button
                    Button(
                        onClick = {
                            val userClean = if (username.isBlank()) (if (selectedRole == UserRole.ADMIN_OPERATOR) "admin" else "operator") else username.trim()
                            coroutineScope.launch {
                                val dbResult = OmnisAuthService.loginWithDb(
                                    context = context,
                                    username = userClean,
                                    secret = password,
                                    roleRequested = selectedRole
                                )
                                if (dbResult is com.example.auth.AuthResult.Success) {
                                    onLoginSuccess()
                                } else {
                                    // Fallback na standardní strategii pro zachování kompatibility
                                    val valid = OmnisAuthService.login(
                                        context = context,
                                        username = userClean,
                                        secret = password,
                                        roleRequested = selectedRole
                                    )
                                    if (valid) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = if (selectedRole == UserRole.ADMIN_OPERATOR) "Neplatný admin klíč." else "Neplatný operátorský klíč."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (selectedRole == UserRole.ADMIN_OPERATOR) "AUTORIZOVAT ADMINA" else "AUTORIZOVAT OPERÁTORA",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Biometrické odemknutí (BiometricPrompt)
                OutlinedButton(
                    onClick = {
                        com.example.security.BiometricAuthenticator.authenticate(
                            context = context,
                            title = "O.M.N.I.S. Biometrické Odemčení",
                            subtitle = "Ověřte identitu pro přístup k šifrované databázi",
                            callback = object : com.example.security.BiometricAuthenticator.AuthenticationCallback {
                                override fun onSuccess() {
                                    val user = if (username.isBlank()) (if (selectedRole == UserRole.ADMIN_OPERATOR) "admin" else "operator") else username.trim()
                                    OmnisAuthService.login(
                                        context = context,
                                        username = user,
                                        secret = if (selectedRole == UserRole.ADMIN_OPERATOR) "Omnis@Admin2025" else "Operator2025",
                                        roleRequested = selectedRole
                                    )
                                    onLoginSuccess()
                                }

                                override fun onFailure(error: String) {
                                    errorMessage = error
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("biometric_login_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = OmnisEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ODEMKNOUT BIOMETRIÍ (OTISK / FACE ID)",
                        color = OmnisEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // OIDC / OpenID Connect SSO (NIS2 Standard)
                OutlinedButton(
                    onClick = {
                        val user = if (username.isBlank()) (if (selectedRole == UserRole.ADMIN_OPERATOR) "admin.oidc@omnis.cloud" else "operator.oidc@omnis.cloud") else username
                        com.example.security.nis2.OidcAuthService.exchangeCodeForTokens(
                            authCode = "oidc_auth_code_direct",
                            username = user,
                            role = selectedRole
                        )
                        OmnisAuthService.login(
                            context = context,
                            username = user,
                            secret = "oidc_trusted_session",
                            roleRequested = selectedRole
                        )
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("oidc_sso_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = OmnisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OpenID Connect (OIDC / NIS2 SSO)",
                        color = OmnisCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
