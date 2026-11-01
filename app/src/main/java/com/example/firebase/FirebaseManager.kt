package com.example.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

object FirebaseManager {
    private var db: FirebaseFirestore? = null

    fun getFirestore(context: Context): FirebaseFirestore {
        if (db == null) {
            val databaseId = context.getString(R.string.firestore_database_id)
            db = FirebaseFirestore.getInstance(databaseId)
        }
        return db!!
    }

    fun getAuth(): FirebaseAuth {
        return Firebase.auth
    }
}
