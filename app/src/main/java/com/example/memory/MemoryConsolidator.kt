package com.example.memory

import android.util.Log
import com.example.api.OmnisGeminiClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Motor pro autonomní konsolidaci a kompresi paměti
 */
object MemoryConsolidator {
    private const val TAG = "MemoryConsolidator"

    suspend fun consolidate(records: List<OmnisRecord>, dao: OmnisDao): Boolean = withContext(Dispatchers.IO) {
        if (records.size < 10) {
            Log.i(TAG, "Nedostatek dat pro konsolidaci (${records.size}/10)")
            return@withContext false
        }

        try {
            val contextToCompress = records.joinToString("\n---\n") { 
                "${it.role.uppercase()}: ${it.content.take(500)}"
            }

            val prompt = """
                Jsi O.M.N.I.S. Memory Auditor. Tvým úkolem je zkomprimovat následující historii konverzace do jednoho sémantického fragmentu (Knowledge Fragment).
                
                Cíl:
                1. Zachytit hlavní fakta, rozhodnutí a naučené koncepty.
                2. Odstranit balast a duplicitu.
                3. Vytvořit titulek a strukturovaný souhrn.
                
                Odpověz striktně v JSON formátu:
                {
                    "title": "Stručný název fragmentu",
                    "summary": "Komplexní, ale zahuštěný souhrn faktů a kontextu...",
                    "tags": ["tag1", "tag2"]
                }
                
                HISTORIE K PROVĚŘENÍ:
                $contextToCompress
            """.trimIndent()

            // Použijeme standardní syntézu pro generování souhrnu
            val result = OmnisGeminiClient.synthesize(prompt, "MEMORY_CONSOLIDATION")
            
            // Extrahovat JSON ze syntézy (Gemini v synthesize vrací answer jako text, ale můžeme ho zkusit parsovat)
            val json = try {
                val start = result.answer.indexOf("{")
                val end = result.answer.lastIndexOf("}") + 1
                if (start >= 0 && end > start) {
                    JSONObject(result.answer.substring(start, end))
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            if (json != null) {
                val fragment = MemoryFragment(
                    title = json.optString("title", "Automatická konsolidace"),
                    summary = json.optString("summary", result.answer),
                    sourceRecordCount = records.size,
                    tags = json.optJSONArray("tags")?.let { arr ->
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) list.add(arr.getString(i))
                        list.joinToString(",")
                    } ?: "omnis,memory"
                )
                
                dao.insertFragment(fragment)
                Log.i(TAG, "Paměťový fragment vytvořen: ${fragment.title}")
                return@withContext true
            } else if (result.answer.isNotBlank()) {
                val fallbackFragment = MemoryFragment(
                    title = "Konsolidovaný souhrn (${records.size} záznamů)",
                    summary = result.answer.take(1500),
                    sourceRecordCount = records.size,
                    tags = "omnis,memory,summary"
                )
                dao.insertFragment(fallbackFragment)
                Log.i(TAG, "Záložní paměťový fragment vytvořen: ${fallbackFragment.title}")
                return@withContext true
            }

            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Konsolidace paměti selhala", e)
            return@withContext false
        }
    }
}
