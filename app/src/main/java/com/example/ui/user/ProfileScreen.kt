package com.example.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.UserProfileUpdate
import com.example.ui.OmnisViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: OmnisViewModel
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isProfileLoading.collectAsStateWithLifecycle()
    
    var isEditMode by remember { mutableStateOf(false) }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var avatarUrl by remember(profile) { mutableStateOf(profile?.avatarUrl ?: "") }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp)
    ) {
        if (isLoading && profile == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = OmnisCyan
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = OmnisPanelDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(OmnisCyan, OmnisViolet)))
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = profile?.username ?: "Uživatel",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = profile?.email ?: "",
                            color = OmnisCyan,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OmnisBgDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ROLE: ${profile?.role ?: "UNKNOWN"}",
                                color = OmnisEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Details Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = OmnisPanelDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OSOBNÍ ÚDAJE",
                                color = OmnisTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            
                            IconButton(onClick = { 
                                if (isEditMode) {
                                    viewModel.updateUserProfile(UserProfileUpdate(bio = bio, avatarUrl = avatarUrl))
                                }
                                isEditMode = !isEditMode 
                            }) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Save else Icons.Default.Edit,
                                    contentDescription = "Editovat",
                                    tint = OmnisCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isEditMode) {
                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Biografie / Poznámky") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OmnisCyan,
                                    unfocusedBorderColor = OmnisBorderDark,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            OutlinedTextField(
                                value = avatarUrl,
                                onValueChange = { avatarUrl = it },
                                label = { Text("URL profilové fotky") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OmnisCyan,
                                    unfocusedBorderColor = OmnisBorderDark,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        } else {
                            ProfileInfoItem("Biografie", profile?.bio ?: "Nespecifikováno")
                            ProfileInfoItem("Datum narození", profile?.birthDate?.let { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it) } ?: "Neuvedeno")
                            ProfileInfoItem("Členem od", profile?.createdAt?.let { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it) } ?: "-")
                        }
                    }
                }

                // Security & Actions
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF5252))
                    Spacer(Modifier.width(8.dp))
                    Text("ODHLÁSIT SE ZE SYSTÉMU", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = OmnisTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Divider(color = OmnisBorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
    }
}
