package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OmnisNavigationState {
    private val _showDeleteConfirm = MutableStateFlow(false)
    val showDeleteConfirm: StateFlow<Boolean> = _showDeleteConfirm.asStateFlow()

    private val _showDevLockDialog = MutableStateFlow(false)
    val showDevLockDialog: StateFlow<Boolean> = _showDevLockDialog.asStateFlow()

    private val _devPassword = MutableStateFlow("")
    val devPassword: StateFlow<String> = _devPassword.asStateFlow()

    private val _devUnlocked = MutableStateFlow(false)
    val devUnlocked: StateFlow<Boolean> = _devUnlocked.asStateFlow()

    fun setShowDeleteConfirm(show: Boolean) { _showDeleteConfirm.value = show }
    fun setShowDevLockDialog(show: Boolean) { _showDevLockDialog.value = show }
    fun setDevPassword(password: String) { _devPassword.value = password }
    
    fun unlockDev(password: String): Boolean {
        if (password == "omnis2026") {
            _devUnlocked.value = true
            _showDevLockDialog.value = false
            _devPassword.value = ""
            return true
        }
        return false
    }
}
