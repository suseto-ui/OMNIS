package com.example

import com.example.api.OmnisGeminiClient
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiDispatcherTest {
    @Test
    fun `test GeminiClient uses IO dispatcher`() {
        val client = OmnisGeminiClient
        assertEquals(Dispatchers.IO, client.ioDispatcher)
    }
}
