package com.example.domain.vector

import kotlin.math.sqrt

/**
 * Lokální vektorový vyhledávací a sémantický embedding engine.
 * Poskytuje nativní offline vektorové reprezentace textu a výpočet kosínové podobnosti
 * bez nutnosti externího cloudového API.
 */
object LocalVectorEngine {
    private const val VECTOR_DIMENSION = 64

    /**
     * Vytvoří normalizovaný 64-dimenzionální sémantický embedding vektor z textu
     * pomocí deterministického n-gram a tokenového hashing algoritmu s frekvenčním vážením.
     */
    fun createEmbedding(text: String): FloatArray {
        val vector = FloatArray(VECTOR_DIMENSION)
        if (text.isBlank()) return vector

        val tokens = text.lowercase()
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length > 1 }

        if (tokens.isEmpty()) return vector

        for (token in tokens) {
            val weight = 1.0f + (token.length.coerceAtMost(10) * 0.1f)
            
            // Primární token hash
            val hash1 = (token.hashCode() and 0x7FFFFFFF) % VECTOR_DIMENSION
            vector[hash1] += weight

            // Sekundární bi-gram projekce pro sémantickou vazbu
            for (i in 0 until token.length - 1) {
                val bigram = token.substring(i, i + 2)
                val hash2 = (bigram.hashCode() and 0x7FFFFFFF) % VECTOR_DIMENSION
                vector[hash2] += weight * 0.35f
            }
        }

        // L2 normalizace vektoru
        var sumSquares = 0.0f
        for (v in vector) {
            sumSquares += v * v
        }

        val norm = sqrt(sumSquares)
        if (norm > 0.0001f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }

        return vector
    }

    /**
     * Spočítá kosínovou podobnost (Cosine Similarity) mezi dvěma normalizovanými vektory.
     * Výsledek je v rozsahu 0.0f až 1.0f.
     */
    fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size || vectorA.isEmpty()) return 0.0f

        var dotProduct = 0.0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
        }

        return dotProduct.coerceIn(0.0f, 1.0f)
    }

    /**
     * Vyhodnotí sémantickou relevanci dotazu vůči textovému obsahu.
     */
    fun evaluateSimilarity(query: String, content: String): Float {
        val queryVector = createEmbedding(query)
        val contentVector = createEmbedding(content)
        return cosineSimilarity(queryVector, contentVector)
    }
}
