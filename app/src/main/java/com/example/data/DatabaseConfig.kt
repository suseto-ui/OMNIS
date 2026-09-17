package com.example.data

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager

object DatabaseConfig {
    private const val TAG = "DatabaseConfig"

    val isTesting: Boolean by lazy {
        try {
            Class.forName("org.junit.Test")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun getBuildConfigValue(fieldName: String): String? {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField(fieldName)
            field.get(null) as? String
        } catch (e: Exception) {
            null
        }
    }

    // Secure, compile-safe exposure of PostgreSQL database connection properties
    val host: String? by lazy { getBuildConfigValue("CLOUDSQL_HOST") }
    val dbName: String by lazy { getBuildConfigValue("CLOUDSQL_DB") ?: "omnis_db" }
    val user: String by lazy { getBuildConfigValue("CLOUDSQL_USER") ?: "postgres" }
    val password: String? by lazy { getBuildConfigValue("CLOUDSQL_PASSWORD") }

    /**
     * Checks if the required environment credentials for Google Cloud SQL PostgreSQL are provided.
     */
    val isCloudSqlConfigured: Boolean
        get() = !host.isNullOrBlank() && !password.isNullOrBlank()

    /**
     * Establishes a direct JDBC Connection to the configured Google Cloud SQL PostgreSQL instance.
     */
    fun getPostgresConnection(): Connection? {
        if (isTesting) {
            return null
        }
        val currentHost = host
        val currentPass = password
        if (currentHost.isNullOrBlank() || currentPass.isNullOrBlank()) {
            Log.w(TAG, "PostgreSQL host or password environment variable is missing. Persisting to local Room only.")
            return null
        }

        val url = "jdbc:postgresql://$currentHost:5432/$dbName"
        return try {
            Class.forName("org.postgresql.Driver")
            // Apply a short connection timeout (e.g., 3 seconds) to prevent long blocks
            DriverManager.setLoginTimeout(3)
            DriverManager.getConnection(url, user, currentPass)
        } catch (e: Exception) {
            Log.w(TAG, "Google Cloud SQL PostgreSQL connection failed (Network is offline or Cloud SQL firewall blocked access: ${e.message}). Falling back safely to local Room database.")
            null
        }
    }
}
