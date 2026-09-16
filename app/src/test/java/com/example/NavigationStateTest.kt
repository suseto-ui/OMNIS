package com.example

import com.example.ui.OmnisNavigationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationStateTest {

    @Test
    fun `test DEV unlock logic`() {
        val state = OmnisNavigationState()
        
        // Initial state
        assertFalse(state.devUnlocked.value)
        
        // Wrong password
        val wrongResult = state.unlockDev("wrong_pass")
        assertFalse(wrongResult)
        assertFalse(state.devUnlocked.value)
        
        // Correct password
        state.setDevPassword("omnis2026")
        state.setShowDevLockDialog(true)
        val correctResult = state.unlockDev("omnis2026")
        
        assertTrue(correctResult)
        assertTrue(state.devUnlocked.value)
        assertEquals("", state.devPassword.value) // password should be cleared
        assertFalse(state.showDevLockDialog.value) // dialog should be closed
    }
}
