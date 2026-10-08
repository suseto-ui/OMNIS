package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.transparency.AiDisclosureManager
import com.example.auth.UserRole
import com.example.data.OmnisRecord
import com.example.ui.admin.AdminNavigationLayout
import com.example.ui.user.UserNavigationLayout

/**
 * Hlavní vstupní obrazovka systému O.M.N.I.S.
 * Zajišťuje zobrazení dialogu transparentnosti AI, přihlašovací obrazovky
 * a směrování do specializovaného uživatelského (UserNavigationLayout)
 * nebo administrátorského (AdminNavigationLayout) rozhraní.
 */
@Composable
fun OmnisMainScreen(
    viewModel: OmnisViewModel,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val showDisclosureDialog by AiDisclosureManager.showDisclosureDialog.collectAsStateWithLifecycle()

    if (showDisclosureDialog) {
        AiDisclosureDialog(
            onConfirmed = {
                viewModel.sendQuery()
            },
            onDismiss = {
                AiDisclosureManager.dismissDialog()
            }
        )
    }

    if (!isAuthenticated) {
        OmnisLoginScreen(
            onLoginSuccess = {
                viewModel.onUserLoggedIn()
            }
        )
        return
    }

    if (currentRole == UserRole.ADMIN_OPERATOR) {
        AdminNavigationLayout(
            viewModel = viewModel,
            onSpeak = onSpeak,
            onExportPdf = onExportPdf
        )
    } else {
        UserNavigationLayout(
            viewModel = viewModel,
            onSpeak = onSpeak,
            onExportPdf = onExportPdf
        )
    }
}
