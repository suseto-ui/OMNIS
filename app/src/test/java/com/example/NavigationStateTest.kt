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
    fun `test delete confirm dialog state`() {
        val state = OmnisNavigationState()
        
        assertFalse(state.showDeleteConfirm.value)
        
        state.setShowDeleteConfirm(true)
        assertTrue(state.showDeleteConfirm.value)
    }
}
