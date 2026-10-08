package com.example.domain.usecase

import com.example.ui.octagon.AnomalyAlert
import com.example.ui.octagon.DomainFrictionPair
import com.example.ui.octagon.Omnis8dFusionEngine
import com.example.ui.octagon.Omnis8dVector

/**
 * Strukturovaný návrh kompenzačního cíle vytvořený na základě detekované anomálie / frikce.
 */
data class CompensatoryGoalDraft(
    val title: String,
    val description: String,
    val domainKey: String,
    val conflictSummary: String,
    val frictionScore: Float,
    val recommendedTasks: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Use Case pro detekci statistických odchylek (anomálií), pnutí mezi doménami
 * a automatické generování návrhů kompenzačních cílů pro uživatele.
 */
class AnomaliesDetectionUseCase {

    /**
     * Vyhodnotí anomálie vůči klouzavému průměru.
     */
    fun detectAnomalies(
        currentVector: Omnis8dVector,
        history: List<Omnis8dVector>
    ): List<AnomalyAlert> {
        return Omnis8dFusionEngine.detectAnomalies(history, currentVector)
    }

    /**
     * Vypočítá pnutí napříč 28 páry dimenzí.
     */
    fun evaluateFrictions(vector: Omnis8dVector): List<DomainFrictionPair> {
        return Omnis8dFusionEngine.evaluateFriction(vector)
    }

    /**
     * Vytvoří návrh kompenzačního cíle, pokud je zjištěno kritické tření nebo anomálie.
     */
    fun generateCompensatoryProposal(
        frictions: List<DomainFrictionPair>,
        anomalies: List<AnomalyAlert>
    ): CompensatoryGoalDraft? {
        // Hledáme kritické tření s frictionScore >= 0.45f
        val criticalFriction = frictions.firstOrNull { it.frictionScore >= 0.45f }
        if (criticalFriction != null) {
            val title = "Stabilizace pnutí: ${criticalFriction.dim1Name} ↔ ${criticalFriction.dim2Name}"
            val desc = "Autonomně navržený kompenzační plán pro vyřešení tření mezi ${criticalFriction.dim1Name} a ${criticalFriction.dim2Name} (skóre pnutí ${(criticalFriction.frictionScore * 100).toInt()}%)."
            val tasks = listOf(
                "1. Audit zdrojů a alokace pro doménu ${criticalFriction.dim1Name}",
                "2. Implementace bezpečnostních a regulačních limitů pro ${criticalFriction.dim2Name}",
                "3. Rekalibrace parametrů v 8D Matici a re-evaluace dopadu",
                "4. Verifikace snížení systémové frikce pod 30%"
            )
            return CompensatoryGoalDraft(
                title = title,
                description = desc,
                domainKey = "${criticalFriction.dim1Key}_${criticalFriction.dim2Key}".uppercase(),
                conflictSummary = criticalFriction.description,
                frictionScore = criticalFriction.frictionScore,
                recommendedTasks = tasks
            )
        }

        // Pokud není párové tření, ale je kritická anomálie v jedné doméně
        val criticalAnomaly = anomalies.firstOrNull { it.sigmaDiff >= 1.8f }
        if (criticalAnomaly != null) {
            val title = "Kompenzace anomálie: ${criticalAnomaly.dimensionName}"
            val desc = "Kritická odchylka v doméně ${criticalAnomaly.dimensionName} (hodnota: ${(criticalAnomaly.currentValue * 100).toInt()}%, odchylka: +${"%.1f".format(criticalAnomaly.sigmaDiff)}σ)."
            val tasks = listOf(
                "1. Zkontrolovat primární příčinu výkyvu v doméně ${criticalAnomaly.dimensionName}",
                "2. Aplikovat stabilizační opatření: ${criticalAnomaly.recommendation}",
                "3. Zapsat výsledek kompenzace do Znalostního Nexusu"
            )
            return CompensatoryGoalDraft(
                title = title,
                description = desc,
                domainKey = criticalAnomaly.dimensionKey.uppercase(),
                conflictSummary = "Statistický skok ${criticalAnomaly.dimensionName}: ${criticalAnomaly.recommendation}",
                frictionScore = (criticalAnomaly.sigmaDiff / 3.0f).coerceIn(0.45f, 0.95f),
                recommendedTasks = tasks
            )
        }

        return null
    }
}
