package com.example.agent

import android.util.Log
import com.example.api.OmnisGeminiClient
import com.example.data.OmnisGoal
import com.example.data.OmnisTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.json.JSONArray
import org.json.JSONObject

/**
 * Engine pro dekonstrukci cílů na atomické úkoly pomocí Gemini
 */
object GoalDeconstructor {
    private const val TAG = "GoalDeconstructor"

    suspend fun deconstruct(goalTitle: String, goalDescription: String): List<OmnisTask> {
        val prompt = """
            Jsi O.M.N.I.S. Architect. Rozlož následující cíl na 3-5 konkrétních, exekutivních úkolů.
            
            CÍL: $goalTitle
            POPIS: $goalDescription
            
            Pro každý úkol urči:
            1. Unikátní ID (string)
            2. Název úkolu
            3. Detailní popis
            4. Přiřazený agent (vyber z: ARCH-SENTINEL, ECON-STRATEGIST, ETHIC-OBSERVER, PHYS-ARCHITECT)
            5. Doména (SYS, ECON, SEC, SOC, ECO, LAW, PHYS, PSYCH)
            
            Vrať POUZE validní JSON pole objektů.
            Příklad:
            [{"id": "t1", "title": "Analýza rizik", "description": "...", "assignedAgentId": "ARCH-SENTINEL", "domain": "SEC"}]
        """.trimIndent()

        return try {
            val result = OmnisGeminiClient.synthesize(prompt, "SYS")
            val jsonArray = JSONArray(result.answer.substringAfter("[").substringBeforeLast("]") + "]")
            val tasks = mutableListOf<OmnisTask>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                tasks.add(OmnisTask(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    status = "PENDING",
                    assignedAgentId = obj.getString("assignedAgentId"),
                    domain = obj.getString("domain")
                ))
            }
            tasks
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při dekonstrukci cíle", e)
            emptyList()
        }
    }
}
