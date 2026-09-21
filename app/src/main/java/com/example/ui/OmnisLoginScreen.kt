package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import com.example.auth.OmnisAuthService
import com.example.auth.UserRole
import com.example.ui.theme.*

/**
 * Centrální přihlašovací rozhraní s podporou sessions, RBAC schématu (Admin / Operátor vs Běžný Uživatel)
 * a tokenového ověření.
 */
@Composable
fun OmnisLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STANDARD_USER) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                        text = "Action-Driven Cognitive Platform • RBAC Auth",
                        color = OmnisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 4.dp))

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
                            text = if (selectedRole == UserRole.ADMIN_OPERATOR) {
                                "ADMIN: Plná správa systému, jističů a administrátorských nástrojů."
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

                if (selectedRole == UserRole.ADMIN_OPERATOR) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Admin Heslo") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OmnisAmber) },
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
                            focusedBorderColor = OmnisAmber,
                            unfocusedBorderColor = OmnisBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = OmnisAmber,
                            unfocusedLabelColor = OmnisTextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_field")
                    )
                } else {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Přístupový klíč Operátora") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OmnisCyan) },
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
                            focusedBorderColor = OmnisCyan,
                            unfocusedBorderColor = OmnisBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = OmnisCyan,
                            unfocusedLabelColor = OmnisTextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_field")
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

                // Submit Button
                Button(
                    onClick = {
                        val valid = OmnisAuthService.login(
                            context = context,
                            username = if (username.isBlank()) (if (selectedRole == UserRole.ADMIN_OPERATOR) "Admin-Root" else "Operátor-User") else username,
                            secret = password,
                            roleRequested = selectedRole
                        )
                        if (valid) {
                            onLoginSuccess()
                        } else {
                            errorMessage = if (selectedRole == UserRole.ADMIN_OPERATOR) "Neplatný admin klíč." else "Neplatný operátorský klíč."
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
        }
    }
}
