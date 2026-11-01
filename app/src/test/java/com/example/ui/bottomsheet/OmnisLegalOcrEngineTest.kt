package com.example.ui.bottomsheet

import org.junit.Assert.*
import org.junit.Test

class OmnisLegalOcrEngineTest {

    @Test
    fun testValidIcoValidation() {
        // Alza.cz a.s. IČO: 27082440
        val result = OmnisLegalOcrEngine.validateIco("27082440")
        assertTrue("Platné IČO by mělo projít", result.isSuccess)
        assertEquals("27082440", result.getOrNull())
    }

    @Test
    fun testInvalidIcoChecksum() {
        // Změněná poslední číslice
        val result = OmnisLegalOcrEngine.validateIco("27082441")
        assertTrue("Neplatné IČO musí selhat", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("neplatný kontrolní součet") == true)
    }

    @Test
    fun testInvalidIcoLength() {
        val result = OmnisLegalOcrEngine.validateIco("12345")
        assertTrue("IČO s nesprávnou délkou musí selhat", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("CHYBA ZADÁNÍ") == true)
    }

    @Test
    fun testValidCzIban() {
        val iban = "CZ6508000000001920001453"
        val result = OmnisLegalOcrEngine.validateIban(iban)
        assertTrue("Platný CZ IBAN o 24 znacích musí projít", result.isSuccess)
    }

    @Test
    fun testInvalidIbanLength() {
        val result = OmnisLegalOcrEngine.validateIban("CZ1234567")
        assertTrue("Příliš krátký IBAN musí selhat", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("CHYBA ZADÁNÍ") == true)
    }

    @Test
    fun testOcrNoiseCleaning() {
        val noisyText = "Smlouva lČO: 27082440 dle §  2161 odst.  2 OZ."
        val cleaned = OmnisLegalOcrEngine.cleanOcrNoise(noisyText)
        assertTrue("lČO by mělo být nahrazeno za IČO", cleaned.contains("IČO: 27082440"))
        assertTrue("Mezery u § by měly být opraveny", cleaned.contains("§ 2161 odst. 2 OZ"))
    }

    @Test
    fun testSunkCostLimiter() {
        val warningUnder500 = OmnisLegalOcrEngine.checkSunkCost(350.0)
        assertNotNull("Při částce do 500 Kč musí naskočit Sunk Cost varování", warningUnder500)
        assertTrue(warningUnder500!!.contains("VAROVÁNÍ Sunk Cost"))

        val warningOver500 = OmnisLegalOcrEngine.checkSunkCost(1500.0)
        assertNull("Při částce nad 500 Kč se Sunk Cost neuplatní", warningOver500)
    }

    @Test
    fun testContractClauseDetection() {
        val contract = """
            Dodavatel nenese odpovědnost za vady věci po uplynutí 6 měsíců.
            Zkrácení záruky na 1 rok.
            Případné spory se řeší dle rozhodčí doložky před rozhodcem určeným prodávajícím.
        """.trimIndent()

        val detected = OmnisLegalOcrEngine.analyzeContractClauses(contract)
        assertTrue("Měly by být detekovány minimálně 3 zakázané klauzule", detected.size >= 3)
        assertTrue(detected.any { it.legalBasis.contains("2161") })
        assertTrue(detected.any { it.legalBasis.contains("2165") })
        assertTrue(detected.any { it.clauseTitle.contains("Rozhodčí doložka") })
    }

    @Test
    fun testMultiChannelClaimCompilation() {
        val multiChannel = OmnisLegalOcrEngine.compileMultiChannelClaim(
            subjectTitle = "Reklamace OLED TV",
            opponentParty = "Elektro Mega s.r.o.",
            claimAmountCzk = 18990.0,
            events = listOf(
                Triple("15. 10. 2026", "Převzetí zboží", "Faktura #98124"),
                Triple("28. 10. 2026", "Výskyt vady displeje", "Fotodokumentace")
            )
        )

        assertNotNull(multiChannel.formalDocument)
        assertNotNull(multiChannel.emailBody)
        assertTrue(multiChannel.formalDocument.contains("VÝZVA K NÁPRAVĚ"))
        assertTrue(multiChannel.formalDocument.contains("čl. 15 GDPR"))
        assertTrue(multiChannel.formalDocument.contains("Chronologie a přílohy [DATUM - UDÁLOST - DŮKAZ]"))
        assertTrue(multiChannel.formalDocument.contains("15. 10. 2026 - Převzetí zboží - Faktura #98124"))
        assertTrue(multiChannel.emailBody.contains("Předmět: Výzva k plnění a reklamace"))
    }
}
