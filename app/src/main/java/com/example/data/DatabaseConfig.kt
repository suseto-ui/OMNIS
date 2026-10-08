package com.example.data

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager

object DatabaseConfig {
    private const val TAG = "DatabaseConfig"

    // Manually force offline testing / deterministic local synthesis mode to avoid API key limits
    var forceTesting: Boolean = false

    val isTesting: Boolean
        get() = forceTesting || try {
            Class.forName("org.junit.Test")
            val vendor = System.getProperty("java.vendor") ?: ""
            val isAndroid = vendor.contains("Android", ignoreCase = true)
            val isRobolectric = System.getProperty("org.robolectric.active") != null
            val isRoborazzi = System.getProperties().keys.any { it.toString().contains("roborazzi") }
            !isAndroid || isRobolectric || isRoborazzi
        } catch (e: ClassNotFoundException) {
            false
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
    private fun getConfigOrEnv(fieldName: String): String? {
        return getBuildConfigValue(fieldName)
            ?: System.getenv(fieldName)?.takeIf { it.isNotBlank() }
            ?: System.getProperty(fieldName)?.takeIf { it.isNotBlank() }
    }

    private val parsedDbUrl: Map<String, String> by lazy {
        val rawUrl = getConfigOrEnv("DATABASE_URL") ?: ""
        if (rawUrl.isBlank()) return@lazy emptyMap()
        try {
            val cleanUrl = rawUrl.replace("postgresql+asyncpg://", "postgresql://")
                .replace("postgres://", "postgresql://")
            val uri = java.net.URI(cleanUrl)
            val userInfo = uri.userInfo?.split(":") ?: emptyList()
            val user = if (userInfo.isNotEmpty()) java.net.URLDecoder.decode(userInfo[0], "UTF-8") else null
            val pass = if (userInfo.size > 1) java.net.URLDecoder.decode(userInfo[1], "UTF-8") else null
            val h = uri.host
            val p = if (uri.port > 0) uri.port.toString() else "5432"
            val db = uri.path?.removePrefix("/")
            buildMap {
                user?.let { put("user", it) }
                pass?.let { put("password", it) }
                h?.let { put("host", it) }
                p?.let { put("port", it) }
                db?.let { put("dbName", it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nepodařilo se parsovat DATABASE_URL: ${e.message}")
            emptyMap()
        }
    }

    val host: String by lazy {
        getConfigOrEnv("CLOUDSQL_HOST")
            ?: parsedDbUrl["host"]
            ?: "34.78.59.190" // Default Google Cloud SQL instance host
    }

    val port: String by lazy {
        getConfigOrEnv("CLOUDSQL_PORT")
            ?: parsedDbUrl["port"]
            ?: "5432"
    }

    val dbName: String by lazy {
        getConfigOrEnv("CLOUDSQL_DB")
            ?: parsedDbUrl["dbName"]
            ?: "omnis_db"
    }

    val user: String by lazy {
        getConfigOrEnv("CLOUDSQL_USER")
            ?: parsedDbUrl["user"]
            ?: "postgres"
    }

    val password: String? by lazy {
        getConfigOrEnv("CLOUDSQL_PASSWORD")
            ?: parsedDbUrl["password"]
    }

    /**
     * Checks if the required environment credentials for Google Cloud SQL PostgreSQL are provided.
     */
    val isCloudSqlConfigured: Boolean
        get() = !host.isBlank() && !password.isNullOrBlank()

    /**
     * Establishes a direct JDBC Connection to the configured Google Cloud SQL PostgreSQL instance.
     */
    fun getPostgresConnection(): Connection? {
        if (isTesting) {
            return null
        }
        val currentHost = host
        val currentPass = password
        if (currentPass.isNullOrBlank()) {
            Log.w(TAG, "PostgreSQL password not provided. Operating in safe local Room cache mode.")
            return null
        }

        var sslParams = ""
        val searchDirs = listOf(java.io.File("/certs"), java.io.File("certs"), java.io.File("."))
        for (dir in searchDirs) {
            val rootCert = java.io.File(dir, "server-ca.pem")
            val clientCert = java.io.File(dir, "client-cert.pem")
            val keyPk8 = java.io.File(dir, "client-key.pk8")
            val keyPem = java.io.File(dir, "client-key.pem")
            val keyFile = if (keyPk8.exists()) keyPk8 else if (keyPem.exists()) keyPem else null

            if (rootCert.exists() && clientCert.exists() && keyFile != null) {
                sslParams = "?sslmode=verify-ca&sslrootcert=${rootCert.absolutePath}&sslcert=${clientCert.absolutePath}&sslkey=${keyFile.absolutePath}"
                Log.i(TAG, "Configuring Cloud SQL mTLS certificates from: ${dir.absolutePath}")
                break
            }
        }

        val url = "jdbc:postgresql://$currentHost:$port/$dbName$sslParams"
        return try {
            Class.forName("org.postgresql.Driver")
            // Apply a short connection timeout (3 seconds) to prevent UI blocks
            DriverManager.setLoginTimeout(3)
            DriverManager.getConnection(url, user, currentPass)
        } catch (e: Exception) {
            Log.w(TAG, "Google Cloud SQL PostgreSQL connection failed (Network is offline or Cloud SQL firewall blocked access: ${e.message}). Falling back safely to local Room database.")
            null
        }
    }
}
