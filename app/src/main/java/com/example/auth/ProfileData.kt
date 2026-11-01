package com.example.auth

import java.util.Date

data class UserProfileResponse(
    val id: String,
    val userId: String,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val birthDate: Date?,
    val bio: String?,
    val preferencesJson: Map<String, Any>,
    val role: String,
    val createdAt: Date
)

data class UserProfileUpdate(
    val avatarUrl: String? = null,
    val birthDate: Date? = null,
    val bio: String? = null,
    val preferencesJson: Map<String, Any>? = null
)

sealed class ProfileResult {
    data class Success(val profile: UserProfileResponse) : ProfileResult()
    data class Failure(val reason: String) : ProfileResult()
}
