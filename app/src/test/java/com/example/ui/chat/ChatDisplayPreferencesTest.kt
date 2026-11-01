package com.example.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatDisplayPreferencesTest {

    @Test
    fun testPreferencesToggles() {
        // Toggle Legal Status Bar
        ChatDisplayPreferences.setShowLegalStatusBarAboveInput(true)
        assertTrue(ChatDisplayPreferences.showLegalStatusBarAboveInput.value)
        ChatDisplayPreferences.setShowLegalStatusBarAboveInput(false)
        assertFalse(ChatDisplayPreferences.showLegalStatusBarAboveInput.value)

        // Toggle Reasoning Trace
        ChatDisplayPreferences.setShowReasoningTrace(true)
        assertTrue(ChatDisplayPreferences.showReasoningTrace.value)
        ChatDisplayPreferences.setShowReasoningTrace(false)
        assertFalse(ChatDisplayPreferences.showReasoningTrace.value)

        // Toggle Audit Matrix Grid
        ChatDisplayPreferences.setShowAuditMatrixGrid(true)
        assertTrue(ChatDisplayPreferences.showAuditMatrixGrid.value)
        ChatDisplayPreferences.setShowAuditMatrixGrid(false)
        assertFalse(ChatDisplayPreferences.showAuditMatrixGrid.value)

        // Toggle Confidence Pills
        ChatDisplayPreferences.setShowConfidencePills(true)
        assertTrue(ChatDisplayPreferences.showConfidencePills.value)
        ChatDisplayPreferences.setShowConfidencePills(false)
        assertFalse(ChatDisplayPreferences.showConfidencePills.value)

        // Toggle Interactive Next Steps
        ChatDisplayPreferences.setShowInteractiveNextSteps(false)
        assertFalse(ChatDisplayPreferences.showInteractiveNextSteps.value)
        ChatDisplayPreferences.setShowInteractiveNextSteps(true)
        assertTrue(ChatDisplayPreferences.showInteractiveNextSteps.value)

        // Toggle Cyber-Glass Effect
        ChatDisplayPreferences.setCyberGlassEffectEnabled(false)
        assertFalse(ChatDisplayPreferences.cyberGlassEffectEnabled.value)
        ChatDisplayPreferences.setCyberGlassEffectEnabled(true)
        assertTrue(ChatDisplayPreferences.cyberGlassEffectEnabled.value)
    }
}
