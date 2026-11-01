package com.example.ui.bottomsheet

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Deterministický validační a analytický engine pro právní kontrolu,
 * OCR čištění a generování auditních podání dle českého OZ, GDPR a NIS2.
 */
object OmnisLegalOcrEngine {

    /**
     * Validace českého IČO pomocí kontrolního součtu Modulo 11.
     * Vrátí Result.success(čistéIčo) nebo Result.failure s přesnou chybovou hláškou.
     */
    fun validateIco(input: String): Result<String> {
        val cleaned = input.filter { it.isDigit() }
        if (cleaned.length != 8) {
            return Result.failure(
                IllegalArgumentException("[CHYBA ZADÁNÍ: Zadané IČO nemá platný formát. Opravte zadání.]")
            )
        }
        val weights = intArrayOf(8, 7, 6, 5, 4, 3, 2)
        var sum = 0
        for (i in 0 until 7) {
            sum += cleaned[i].digitToInt() * weights[i]
        }
        val remainder = sum % 11
        val checkDigit = when (remainder) {
            0, 10 -> 1
            1 -> 0
            else -> 11 - remainder
        }
        return if (checkDigit == cleaned[7].digitToInt()) {
            Result.success(cleaned)
        } else {
            Result.failure(
                IllegalArgumentException("[CHYBA ZADÁNÍ: Zadané IČO má neplatný kontrolní součet. Opravte zadání.]")
            )
        }
    }

    /**
     * Validace formátu IBAN (se zaměřením na CZ IBAN).
     */
    fun validateIban(input: String): Result<String> {
        val cleaned = input.replace("\\s".toRegex(), "").uppercase(Locale.getDefault())
        if (!cleaned.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$".toRegex())) {
            return Result.failure(
                IllegalArgumentException("[CHYBA ZADÁNÍ: Zadané IBAN nemá platný formát. Opravte zadání.]")
            )
        }
        if (cleaned.startsWith("CZ") && cleaned.length != 24) {
            return Result.failure(
                IllegalArgumentException("[CHYBA ZADÁNÍ: Zadané české IBAN musí mít přesně 24 znaků. Opravte zadání.]")
            )
        }
        return Result.success(cleaned)
    }

    /**
     * Vyčištění šumu z OCR skenů a rekonstrukce slov dle sémantického kontextu.
     */
    fun cleanOcrNoise(rawOcrText: String): String {
        var cleaned = rawOcrText
        // Oprava typických záměn OCR u IČO / paragrafů / smluvních slov
        cleaned = cleaned.replace("lČO", "IČO")
        cleaned = cleaned.replace("ICO:", "IČO:")
        cleaned = cleaned.replace("lban", "IBAN", ignoreCase = true)
        cleaned = cleaned.replace("§\\s*([0-9]+)".toRegex(), "§ $1")
        cleaned = cleaned.replace("odst\\.\\s*([0-9]+)".toRegex(), "odst. $1")
        cleaned = cleaned.replace("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]".toRegex(), "")
        cleaned = cleaned.replace(" {2,}".toRegex(), " ")
        cleaned = cleaned.replace("\n{3,}".toRegex(), "\n\n")
        return cleaned.trim()
    }

    /**
     * Detekce bagatelního sporu a vyhodnocení Sunk Cost.
     */
    fun checkSunkCost(amountCzk: Double): String? {
        return if (amountCzk in 0.01..500.0) {
            "[VAROVÁNÍ Sunk Cost: Spor o tuto částku se nevyplatí hloubkově analyzovat. Generuji nejrychlejší řešení copy-paste.]"
        } else {
            null
        }
    }

    data class DetectedClauseRisk(
        val clauseTitle: String,
        val legalBasis: String,
        val excerpt: String,
        val explanation: String,
        val isInvalidByLaw: Boolean
    )

    /**
     * Kontrola smluv a VOP na přítomnost neplatných a zneužívajících ustanovení
     * vůči spotřebiteli dle zákona č. 89/2012 Sb. (OZ).
     */
    fun analyzeContractClauses(contractText: String): List<DetectedClauseRisk> {
        val risks = mutableListOf<DetectedClauseRisk>()
        val lower = contractText.lowercase(Locale.getDefault())

        if (lower.contains("vylučuje odpovědnost") || lower.contains("nenese odpovědnost za vady") || lower.contains("zboží se prodává jak stojí a leží")) {
            risks.add(
                DetectedClauseRisk(
                    clauseTitle = "Vyloučení zákonné odpovědnosti za vady",
                    legalBasis = "§ 2161 odst. 2 a § 1814 písm. a) OZ",
                    excerpt = "Ujednání omezující nebo vylučující práva z vadného plnění.",
                    explanation = "Kogentní právo spotřebitele. K ujednáním, která zkracují zákonná práva kupujícího z vadného plnění, se ze zákona nepřihlíží.",
                    isInvalidByLaw = true
                )
            )
        }

        if (lower.contains("zkrácení záruky") || lower.contains("záruka 6 měsíců") || lower.contains("záruka 12 měsíců") || lower.contains("zkracuje se na 1 rok")) {
            risks.add(
                DetectedClauseRisk(
                    clauseTitle = "Zkrácení zákonné lhůty pro vytknutí vady",
                    legalBasis = "§ 2165 OZ",
                    excerpt = "Zkrácení 24měsíční lhůty pro uplatnění práva z vady u nového spotřebního zboží.",
                    explanation = "Lhůta 24 měsíců je pro spotřebitele kogentní minimum. Zkrácení je absolutně neplatné.",
                    isInvalidByLaw = true
                )
            )
        }

        if (lower.contains("rozhodčí doložk") || lower.contains("rozhodčí soud") || lower.contains("rozhodce určený prodávajícím")) {
            risks.add(
                DetectedClauseRisk(
                    clauseTitle = "Rozhodčí doložka ve spotřebitelské smlouvě",
                    legalBasis = "§ 1814 písm. e) OZ a zákon o rozhodčím řízení č. 216/1994 Sb.",
                    excerpt = "Povinnost spotřebitele řešit spor před neveřejným rozhodcem namísto obecného soudu.",
                    explanation = "Ve spotřebitelských smlouvách je rozhodčí doložka zakázaným ujednáním, pokud nebyla sjednána samostatně po vzniku sporu.",
                    isInvalidByLaw = true
                )
            )
        }

        if (lower.contains("důkazní břemeno") || lower.contains("zákazník musí prokázat že vada existovala při převzetí")) {
            risks.add(
                DetectedClauseRisk(
                    clauseTitle = "Nezákonný přesun důkazního břemene",
                    legalBasis = "§ 2161 odst. 5 OZ",
                    excerpt = "Požadavek, aby spotřebitel v prvním roce prokazoval původ vady.",
                    explanation = "Projeví-li se vada v průběhu jednoho roku od převzetí, má se za to, že věc byla vadná již při převzetí. Důkazní břemeno nese výhradně prodávající.",
                    isInvalidByLaw = true
                )
            )
        }

        if (lower.contains("jednostranná změna ceny") || lower.contains("vyhrazuje právo zvýšit cenu bez možnosti odstoupení")) {
            risks.add(
                DetectedClauseRisk(
                    clauseTitle = "Jednostranné zvýšení ceny bez práva na odstoupení",
                    legalBasis = "§ 1814 písm. d) OZ",
                    excerpt = "Ujednání umožňující prodejci navýšit cenu bez práva spotřebitele zrušit smlouvu.",
                    explanation = "Takové ujednání je ze zákona nicotné / zneužívající.",
                    isInvalidByLaw = true
                )
            )
        }

        return risks
    }

    data class MultiChannelAuditResult(
        val formalDocument: String,
        val emailBody: String,
        val timelineChronology: String
    )

    /**
     * Sestaví kompletní 8D právní podání dle strict standardu:
     * 1. Formální dokument (pro ČOI / Finančního arbitra / soud)
     * 2. Průvodní e-mailová zpráva
     * 3. Chronologie a přílohy ve formátu [DATUM - UDÁLOST - DŮKAZ]
     */
    fun compileMultiChannelClaim(
        subjectTitle: String,
        opponentParty: String,
        claimAmountCzk: Double,
        events: List<Triple<String, String, String>> = emptyList()
    ): MultiChannelAuditResult {
        val dateNow = SimpleDateFormat("dd. MM. yyyy", Locale.getDefault()).format(Date())
        val isSunkCost = checkSunkCost(claimAmountCzk)

        val timelineBuilder = StringBuilder()
        timelineBuilder.append("Chronologie a přílohy [DATUM - UDÁLOST - DŮKAZ]:\n")
        if (events.isEmpty()) {
            timelineBuilder.append("• [$dateNow - Zjištění vady a rozporu s kupní smlouvou - Objednávka a faktura]\n")
            timelineBuilder.append("• [$dateNow - Odeslání této výzvy k nápravě a reklamace - Reklamační protokol O.M.N.I.S.]")
        } else {
            events.forEach { (date, event, proof) ->
                timelineBuilder.append("• [$date - $event - $proof]\n")
            }
        }
        val chronologyText = timelineBuilder.toString().trim()

        val formalDoc = buildString {
            if (isSunkCost != null) {
                append("$isSunkCost\n\n")
            }
            append("VÝZVA K NÁPRAVĚ A UPLATNĚNÍ PRÁV Z VADNÉHO PLNĚNÍ\n")
            append("dle zákona č. 89/2012 Sb., občanský zákoník (OZ)\n")
            append("Datum vyhotovení: $dateNow\n")
            append("Adresát (protistrana): $opponentParty\n")
            append("Věc: $subjectTitle | Požadovaná kompenzace: ${String.format(Locale.US, "%.2f", claimAmountCzk)} Kč\n\n")
            append("I. Skutkový stav a právní základ\n")
            append("V souladu s § 2161 odst. 2 a odst. 5 OZ nese prodávající plnou odpovědnost za vady věci při převzetí. Projeví-li se vada v průběhu 12 měsíců od převzetí, uplatňuje se zákonná presumpce vadnosti.\n\n")
            append("II. Nárok a stanovení lhůty\n")
            append("Požaduji bezodkladnou nápravu, resp. vrácení plné kupní ceny ve výši ${String.format(Locale.US, "%.2f", claimAmountCzk)} Kč ve lhůtě 5 pracovních dnů na uvedený bankovní účet.\n\n")
            append("III. Žádost o výpis údajů dle čl. 15 GDPR\n")
            append("Současně v souladu s čl. 15 Nařízení Evropského parlamentu a Rady (EU) 2016/679 (GDPR) požaduji kompletní výpis všech mých osobních údajů zpracovávaných Vaší společností včetně interních záznamů reklamačního řízení.\n\n")
            append("IV. Upozornění na dozorové orgány\n")
            append("Při neposkytnutí nápravy v zákonné lhůtě bude věc neprodleně předána České obchodní inspekci (ČOI), Úřadu pro ochranu osobních údajů (ÚOOÚ) a případně uplatněna u příslušného soudu včetně zákonného úroku z prodlení dle nařízení vlády č. 351/2013 Sb.\n\n")
            append(chronologyText)
        }

        val email = buildString {
            append("Předmět: Výzva k plnění a reklamace – $subjectTitle (O.M.N.I.S. Ref)\n\n")
            append("Vážení,\n\n")
            append("v příloze Vám zasílám formální výzvu k nápravě a uplatnění práv z vadného plnění ve výši ${String.format(Locale.US, "%.2f", claimAmountCzk)} Kč a současně žádost o výpis údajů dle čl. 15 GDPR.\n\n")
            append("Žádám o potvrzení přijetí a vyřízení v zákonné lhůtě.\n\n")
            append("S pozdravem,\nOdesílatel v zastoupení O.M.N.I.S. Legal Guard\n")
            append("Přílohy:\n1. Formalni_Podani_OMNIS.pdf\n2. Reklamacni_Duvody.pdf")
        }

        return MultiChannelAuditResult(
            formalDocument = formalDoc,
            emailBody = email,
            timelineChronology = chronologyText
        )
    }
}
