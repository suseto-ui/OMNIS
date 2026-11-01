package com.example.accessibility

import androidx.compose.ui.Modifier
import com.example.accessibility.OmnisAccessibilityHelper.accessibleButton
import com.example.accessibility.OmnisAccessibilityHelper.accessibleHeading
import com.example.accessibility.OmnisAccessibilityHelper.accessibleInputField
import com.example.accessibility.OmnisAccessibilityHelper.accessibleLiveRegion
import org.junit.Assert.assertNotNull
import org.junit.Test

class WaiAriaAccessibilityTest {

    @Test
    fun testAccessibilityModifiersCreation() {
        val btnMod = Modifier.accessibleButton("Odeslat dotaz do kognitivního jádra", 1f)
        assertNotNull(btnMod)

        val inputMod = Modifier.accessibleInputField("Vstupní pole pro zadání promptu", 0f)
        assertNotNull(inputMod)

        val headingMod = Modifier.accessibleHeading("Hlavní dashboard", 0f)
        assertNotNull(headingMod)

        val liveMod = Modifier.accessibleLiveRegion("Odpověď vygenerována")
        assertNotNull(liveMod)
    }
}
