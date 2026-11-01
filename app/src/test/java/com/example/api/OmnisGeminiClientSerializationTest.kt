package com.example.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OmnisGeminiClientSerializationTest {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `deserializes hybrid response with typed tool call payload`() {
        val json = """
            {
              "intent": "general_assistant",
              "confidence_score": 0.91,
              "execution_plan": [
                {
                  "step_number": 1,
                  "action_description": "Search the semantic index",
                  "tool_call": {
                    "tool_id": "vector_search",
                    "parameters": {
                      "query": "system health",
                      "limit": 2,
                      "filters": {
                        "domain": "ops"
                      }
                    }
                  }
                }
              ],
              "immediate_response": "Prepared the execution plan.",
              "required_output_format": "json"
            }
        """.trimIndent()

        val adapter: JsonAdapter<HybridOmnisResponse> = moshi.adapter(HybridOmnisResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals("vector_search", response!!.executionPlan.first().toolCall?.toolId)
        assertEquals("system health", response.executionPlan.first().toolCall?.parameters?.get("query"))

        val filters = response.executionPlan.first().toolCall?.parameters?.get("filters") as? Map<*, *>
        assertNotNull(filters)
        assertEquals("ops", filters?.get("domain"))
    }
}
