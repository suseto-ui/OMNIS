package com.example.data

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager

object DatabaseConfig {
    private const val TAG = "DatabaseConfig"

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
        val currentHost = host
        val currentPass = password
        if (currentHost.isNullOrBlank() || currentPass.isNullOrBlank()) {
            Log.w(TAG, "PostgreSQL host or password environment variable is missing. Persisting to local Room only.")
            return null
        }

        val url = "jdbc:postgresql://$currentHost:5432/$dbName"
        return try {
            Class.forName("org.postgresql.Driver")
            DriverManager.getConnection(url, user, currentPass)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Google Cloud SQL PostgreSQL at $url", e)
            null
        }
    }
}
