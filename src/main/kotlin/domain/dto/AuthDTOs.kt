package com.codewithfk.domain.dto

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class SignupRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

@Serializable
data class LogoutRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val email:String?
)

@Serializable
data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class UserProfile(
    val userId: String,
    val email: String,
    val createdAt: String
)

@Serializable
data class OkResponse(
    val ok: Boolean = true
)
