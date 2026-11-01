package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

@IgnoreExtraProperties
data class CloudOmnisRecord(
    val id: String = "",
    val userId: String = "",
    val query: String = "",
    val response: String = "",
    val domain: String? = null,
    val confidenceScore: Double? = null,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)

@IgnoreExtraProperties
data class CloudOmnisGoal(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val status: String = "active",
    val progress: Double? = null,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)

class FirebaseFirestoreRepository(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseFirestoreRepo"

        @Volatile
        private var instance: FirebaseFirestoreRepository? = null

        fun getInstance(context: Context): FirebaseFirestoreRepository {
            return instance ?: synchronized(this) {
                instance ?: FirebaseFirestoreRepository(
                    FirebaseFirestore.getInstance(
                        context.applicationContext.getString(R.string.firestore_database_id)
                    )
                ).also { instance = it }
            }
        }
    }

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Uživatel musí být přihlášen přes Google pro přístup k Firestore.")
    }

    /**
     * Reálné sledování záznamů uživatele v reálném čase.
     */
    fun observeUserRecords(): Flow<List<CloudOmnisRecord>> = flow {
        val uid = requireUserId()
        val query = db.collection("users").document(uid).collection("records")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects<CloudOmnisRecord>() }
                .catch { error ->
                    Log.e(TAG, "Chyba při čtení záznamů z Firestore: ${error.message}", error)
                    throw error
                }
        )
    }

    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(source: Flow<T>) {
        source.collect { value -> emit(value) }
    }

    /**
     * Uložení nového kognitivního záznamu do Firestore.
     */
    suspend fun saveRecord(
        id: String,
        query: String,
        response: String,
        domain: String? = null,
        confidenceScore: Double? = null
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("records").document(id)
            val payload = hashMapOf<String, Any>(
                "id" to id,
                "userId" to uid,
                "query" to query,
                "response" to response,
                "createdAt" to FieldValue.serverTimestamp()
            )
            domain?.let { payload["domain"] = it }
            confidenceScore?.let { payload["confidenceScore"] = it }

            docRef.set(payload).await()
            Log.d(TAG, "Záznam $id úspěšně uložen do cloudu")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Selhalo uložení záznamu do Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uložení cíle do cloudu.
     */
    suspend fun saveGoal(
        id: String,
        title: String,
        status: String = "active",
        progress: Double = 0.0
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("goals").document(id)
            val payload = hashMapOf<String, Any>(
                "id" to id,
                "userId" to uid,
                "title" to title,
                "status" to status,
                "progress" to progress,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Selhalo uložení cíle do Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
}
