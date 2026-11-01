package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Uložený profil uživatele s kryptograficky soleným PBKDF2 hashem hesla
 * a přiřazenou RBAC rolí.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: String,
    val createdAt: Long = System.currentTimeMillis()
)
