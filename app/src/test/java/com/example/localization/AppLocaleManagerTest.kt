package com.example.localization

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.OmnisTab
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLocaleManager
import com.example.ui.localization.OmnisStrings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLocaleManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppLocaleManager.initialize(context)
    }

    @Test
    fun `test language toggle switches between Czech and English`() {
        AppLocaleManager.setLanguage(context, AppLanguage.CS)
        assertEquals(AppLanguage.CS, AppLocaleManager.currentLanguage.value)
        assertFalse(AppLocaleManager.isEnglish)

        AppLocaleManager.toggleLanguage(context)
        assertEquals(AppLanguage.EN, AppLocaleManager.currentLanguage.value)
        assertTrue(AppLocaleManager.isEnglish)

        AppLocaleManager.toggleLanguage(context)
        assertEquals(AppLanguage.CS, AppLocaleManager.currentLanguage.value)
        assertFalse(AppLocaleManager.isEnglish)
    }

    @Test
    fun `test OmnisStrings localization returns correct titles for CS and EN`() {
        assertEquals("Kognitivní Chat", OmnisStrings.tabTitle(OmnisTab.CHAT, AppLanguage.CS))
        assertEquals("Cognitive Chat", OmnisStrings.tabTitle(OmnisTab.CHAT, AppLanguage.EN))

        assertEquals("Operační Kokpit", OmnisStrings.tabTitle(OmnisTab.DASHBOARD, AppLanguage.CS))
        assertEquals("Operations Cockpit", OmnisStrings.tabTitle(OmnisTab.DASHBOARD, AppLanguage.EN))

        assertEquals("Produkční Audit & Benchmark", OmnisStrings.tabTitle(OmnisTab.PRODUCTION_AUDIT, AppLanguage.CS))
        assertEquals("Production Audit & Benchmark", OmnisStrings.tabTitle(OmnisTab.PRODUCTION_AUDIT, AppLanguage.EN))

        assertEquals("8D Matice & Octagon", OmnisStrings.tabTitle(OmnisTab.MATRIX, AppLanguage.CS))
        assertEquals("8D Matrix & Octagon", OmnisStrings.tabTitle(OmnisTab.MATRIX, AppLanguage.EN))
    }
}
