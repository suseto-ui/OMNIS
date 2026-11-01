package com.example.agent

import android.util.Log
import com.example.api.OmnisGeminiClient
import com.example.data.OmnisTask
import com.example.ui.OmnisDomain
import org.json.JSONArray

/**
 * Engine pro dekonstrukci cílů na atomické úkoly pomocí Gemini a 8D multi-agentní architektury.
 */
object GoalDeconstructor {
    private const val TAG = "GoalDeconstructor"

    suspend fun deconstruct(goalTitle: String, goalDescription: String): List<OmnisTask> {
        val prompt = """
            Jsi O.M.N.I.S. Architect. Rozlož následující cíl na 3 až 5 konkrétních, exekutivních úkolů s využitím 8D multi-agentní matice.
            
            CÍL: $goalTitle
            POPIS: $goalDescription
            
            Pro každý úkol urči:
            1. Unikátní ID (např. "t1", "t2", "t3")
            2. Název úkolu (title)
            3. Detailní popis (description)
            4. Přiřazený agent (vyber z: sys_sentinel, econ_strategist, psych_aligner, eco_restorer, law_compliance, arch_sentinel, phys_architect, soc_analyst)
            5. Doména (vyber z: Sys, Econ, Psych, Eco, Law, Sec, Phys, Soc)
            
            Vrať VÝHRADNĚ validní JSON pole objektů bez dalšího markdownu.
            Příklad:
            [
              {"id": "t1", "title": "Zero-Trust audit", "description": "Analýza bezpečnostních rizik.", "assignedAgentId": "arch_sentinel", "domain": "Sec"},
              {"id": "t2", "title": "Optimalizace nákladů", "description": "Kalkulace provozní efektivity.", "assignedAgentId": "econ_strategist", "domain": "Econ"}
            ]
        """.trimIndent()

        return try {
            val result = OmnisGeminiClient.synthesize(prompt, "SYSTEMS")
            val raw = result.answer.trim()
            val jsonStart = raw.indexOf('[')
            val jsonEnd = raw.lastIndexOf(']')
            
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                val arrayStr = raw.substring(jsonStart, jsonEnd + 1)
                val jsonArray = JSONArray(arrayStr)
                val tasks = mutableListOf<OmnisTask>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawDomain = obj.optString("domain", "Sys")
                    val normDomain = OmnisDomain.short(rawDomain)
                    tasks.add(OmnisTask(
                        id = obj.optString("id", "task_${System.currentTimeMillis()}_$i"),
                        title = obj.optString("title", "Úkol ${i + 1}"),
                        description = obj.optString("description", ""),
                        status = "PENDING",
                        assignedAgentId = obj.optString("assignedAgentId", "sys_sentinel"),
                        domain = normDomain
                    ))
                }
                if (tasks.isNotEmpty()) return tasks
            }
            createDeterministicTasks(goalTitle, goalDescription)
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při dekonstrukci cíle, aktivován deterministický rozklad", e)
            createDeterministicTasks(goalTitle, goalDescription)
        }
    }

    private fun createDeterministicTasks(title: String, description: String): List<OmnisTask> {
        return listOf(
            OmnisTask(
                id = "det_t1_${System.currentTimeMillis()}",
                title = "Architektonický návrh a systémové modelování",
                description = "Definice komponent a systémových vazeb pro: $title.",
                status = "PENDING",
                assignedAgentId = "sys_sentinel",
                domain = "Sys"
            ),
            OmnisTask(
                id = "det_t2_${System.currentTimeMillis()}",
                title = "Zero-Trust bezpečnostní prověrka",
                description = "Identifikace slabých míst a audit rizik perimetru.",
                status = "PENDING",
                assignedAgentId = "arch_sentinel",
                domain = "Sec"
            ),
            OmnisTask(
                id = "det_t3_${System.currentTimeMillis()}",
                title = "Ekonomická a nákladová optimalizace",
                description = "Analýza ROI, alokace zdrojů a nákladové efektivity.",
                status = "PENDING",
                assignedAgentId = "econ_strategist",
                domain = "Econ"
            ),
            OmnisTask(
                id = "det_t4_${System.currentTimeMillis()}",
                title = "Regulatorní shoda a právní audit",
                description = "Ověření souladu s právními normami a etickými standardy.",
                status = "PENDING",
                assignedAgentId = "law_compliance",
                domain = "Law"
            )
        )
    }
}
