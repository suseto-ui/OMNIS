package com.example.auth

/**
 * Granulární systémová oprávnění (Fine-grained RBAC permissions)
 */
enum class SystemPermission {
    ACCESS_SYSTEM_ACTIONS,
    ACCESS_DEV_DIAGNOSTIC,
    MODIFY_SYSTEM_STATE,
    EXECUTE_DATA_SYNC,
    AUDIT_GOVERNANCE,
    ACCESS_KNOWLEDGE_BASE,
    ACCESS_TELEMETRY
}

/**
 * Behaviorální vzor Strategie (Strategy Pattern):
 * Každý uživatelský profil má dedikovanou třídu implementující toto rozhraní.
 * Zcela eliminuje monolitické if-else / switch řetězce a snižuje cyklomatickou složitost metod na hodnotu 1.
 */
interface UserAuthorizationStrategy {
    val role: UserRole
    val maxConcurrentTransactions: Int

    fun hasPermission(permission: SystemPermission): Boolean
    fun canAccessSystemActions(): Boolean
    fun canAccessDevDiagnostic(): Boolean
    fun canModifySystemState(): Boolean
    fun canExecuteTransaction(transactionType: String): Boolean
    fun validateCredentials(secret: String, hasher: (String) -> String): Boolean
    fun getAllowedEndpoints(): Set<String>
}

/**
 * Strategie pro roli: ADMIN_OPERATOR
 * Plná oprávnění ke všem systémovým zásahům, infra diagnostice a tenzorovým konfiguracím.
 */
class AdminOperatorStrategy : UserAuthorizationStrategy {
    override val role: UserRole = UserRole.ADMIN_OPERATOR
    override val maxConcurrentTransactions: Int = 10

    private val allowedEndpoints = setOf(
        "/api/admin/diagnostics",
        "/api/admin/sync",
        "/api/admin/resilience",
        "/api/admin/governance",
        "/api/admin/memory",
        "/api/cognitive/chat",
        "/api/cognitive/query"
    )

    private val validSecretHashes = setOf(
        "omnis2026",
        "root_operator",
        "admin"
    )

    override fun hasPermission(permission: SystemPermission): Boolean = true

    override fun canAccessSystemActions(): Boolean = true

    override fun canAccessDevDiagnostic(): Boolean = true

    override fun canModifySystemState(): Boolean = true

    override fun canExecuteTransaction(transactionType: String): Boolean = true

    override fun validateCredentials(secret: String, hasher: (String) -> String): Boolean {
        if (secret.isBlank()) return false
        val hashed = hasher(secret.trim())
        return validSecretHashes.any { hasher(it) == hashed }
    }

    override fun getAllowedEndpoints(): Set<String> = allowedEndpoints
}

/**
 * Strategie pro roli: STANDARD_USER
 * Bezpečný přístup ke kognitivnímu asistentovi a znalostní bázi bez možnosti modifikace infrastruktury.
 */
class StandardUserStrategy : UserAuthorizationStrategy {
    override val role: UserRole = UserRole.STANDARD_USER
    override val maxConcurrentTransactions: Int = 3

    private val allowedEndpoints = setOf(
        "/api/cognitive/chat",
        "/api/cognitive/query",
        "/api/cognitive/artifacts",
        "/api/user/profile"
    )

    private val permittedTransactions = setOf(
        "db_connectivity_test",
        "chat_inference",
        "query_memory"
    )

    override fun hasPermission(permission: SystemPermission): Boolean {
        return permission == SystemPermission.ACCESS_KNOWLEDGE_BASE ||
                permission == SystemPermission.ACCESS_TELEMETRY
    }

    override fun canAccessSystemActions(): Boolean = false

    override fun canAccessDevDiagnostic(): Boolean = false

    override fun canModifySystemState(): Boolean = false

    override fun canExecuteTransaction(transactionType: String): Boolean {
        return permittedTransactions.contains(transactionType)
    }

    override fun validateCredentials(secret: String, hasher: (String) -> String): Boolean {
        // Běžný uživatel nevyžaduje operátorský hash klíč
        return true
    }

    override fun getAllowedEndpoints(): Set<String> = allowedEndpoints
}

/**
 * Strategie pro roli: AUDITOR (bezpečnostní a compliance inspektor)
 */
class AuditorAuthorizationStrategy : UserAuthorizationStrategy {
    override val role: UserRole = UserRole.STANDARD_USER
    override val maxConcurrentTransactions: Int = 5

    private val allowedEndpoints = setOf(
        "/api/admin/governance",
        "/api/admin/telemetry",
        "/api/cognitive/query"
    )

    override fun hasPermission(permission: SystemPermission): Boolean {
        return permission == SystemPermission.AUDIT_GOVERNANCE ||
                permission == SystemPermission.ACCESS_TELEMETRY ||
                permission == SystemPermission.ACCESS_KNOWLEDGE_BASE
    }

    override fun canAccessSystemActions(): Boolean = false

    override fun canAccessDevDiagnostic(): Boolean = true

    override fun canModifySystemState(): Boolean = false

    override fun canExecuteTransaction(transactionType: String): Boolean {
        return transactionType == "ai_governance_export" || transactionType == "db_connectivity_test"
    }

    override fun validateCredentials(secret: String, hasher: (String) -> String): Boolean = true

    override fun getAllowedEndpoints(): Set<String> = allowedEndpoints
}

/**
 * Centrální registr strategií autorizace:
 * Poskytuje polymorfní strategii s časovou složitostí O(1) a cyklomatickou složitostí 1.
 */
object UserAuthorizationStrategyRegistry {
    private val strategies: Map<UserRole, UserAuthorizationStrategy> = mapOf(
        UserRole.ADMIN_OPERATOR to AdminOperatorStrategy(),
        UserRole.STANDARD_USER to StandardUserStrategy()
    )

    fun getStrategy(role: UserRole): UserAuthorizationStrategy {
        return strategies[role] ?: StandardUserStrategy()
    }
}
