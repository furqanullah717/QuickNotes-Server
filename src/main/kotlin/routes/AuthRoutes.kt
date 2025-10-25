package com.codewithfk.routes

import com.codewithfk.config.Env
import com.codewithfk.domain.dto.*
import com.codewithfk.services.*
import com.codewithfk.util.ProblemFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.authRoutes() {
    val authService = AuthService()
    val tokenService = TokenService(
        jwtSecret = Env.JWT_SECRET,
        accessTokenTtlMinutes = Env.ACCESS_TOKEN_TTL_MIN,
        refreshTokenTtlDays = Env.REFRESH_TOKEN_TTL_DAYS
    )
    val passwordResetService = PasswordResetService()
    val emailService = EmailService(
        smtpHost = Env.SMTP_HOST,
        smtpPort = Env.SMTP_PORT,
        smtpUser = Env.SMTP_USER,
        smtpPassword = Env.SMTP_PASS,
        fromEmail = Env.EMAIL_FROM
    )
    
    route("/auth") {
        post("/signup") {
            try {
                val request = call.receive<SignupRequest>()
                val user = authService.signup(request.email, request.password)
                
                val accessToken = tokenService.generateAccessToken(user.id.value)
                val refreshToken = tokenService.generateRefreshToken(user.id.value)
                
                try {
                    emailService.sendWelcomeEmail(user.email)
                } catch (e: Exception) {
                    println("Failed to send welcome email: ${e.message}")
                }
                
                call.respond(HttpStatusCode.Created, AuthResponse(
                    userId = user.id.value.toString(),
                    accessToken = accessToken,
                    refreshToken = refreshToken
                ))
            } catch (e: IllegalArgumentException) {
                val problem = ProblemFactory.conflict(e.message ?: "User already exists")
                call.respond(HttpStatusCode.Conflict, problem)
            }
        }
        
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                val user = authService.login(request.email, request.password)
                
                val accessToken = tokenService.generateAccessToken(user.id.value)
                val refreshToken = tokenService.generateRefreshToken(user.id.value)
                
                call.respond(AuthResponse(
                    userId = user.id.value.toString(),
                    accessToken = accessToken,
                    refreshToken = refreshToken
                ))
            } catch (e: IllegalArgumentException) {
                val problem = ProblemFactory.unauthorized("Invalid email or password")
                call.respond(HttpStatusCode.Unauthorized, problem)
            }
        }
        
        post("/refresh") {
            try {
                val request = call.receive<RefreshRequest>()
                val userId = tokenService.validateRefreshToken(request.refreshToken)
                    ?: throw IllegalArgumentException("Invalid refresh token")
                
                val accessToken = tokenService.generateAccessToken(userId)
                val refreshToken = tokenService.generateRefreshToken(userId)
                
                call.respond(RefreshResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken
                ))
            } catch (e: IllegalArgumentException) {
                val problem = ProblemFactory.unauthorized("Invalid refresh token")
                call.respond(HttpStatusCode.Unauthorized, problem)
            }
        }
        
        post("/forgot") {
            try {
                val request = call.receive<ForgotPasswordRequest>()
                val user = authService.getUserByEmail(request.email)
                    ?: throw IllegalArgumentException("User not found")
                
                val resetCode = passwordResetService.generateResetCode(user.id.value)
                
                emailService.sendPasswordResetEmail(user.email, resetCode)
                
                call.respond(OkResponse())
            } catch (e: IllegalArgumentException) {
                call.respond(OkResponse())
            }
        }
        
        post("/reset") {
            try {
                val request = call.receive<ResetPasswordRequest>()
                val user = passwordResetService.validateResetCode(request.email, request.code)
                    ?: throw IllegalArgumentException("Invalid or expired reset code")
                
                if (request.newPassword.length < 8) {
                    throw IllegalArgumentException("Password must be at least 8 characters long")
                }
                authService.updatePassword(user.id.value, request.newPassword)
                passwordResetService.markResetCodeUsed(request.email, request.code)
                
                call.respond(OkResponse())
            } catch (e: IllegalArgumentException) {
                val problem = ProblemFactory.badRequest(e.message ?: "Invalid reset code")
                call.respond(HttpStatusCode.BadRequest, problem)
            }
        }
        
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("sub")?.asString() ?: "")
                val user = authService.getUserById(userId)
                    ?: throw IllegalArgumentException("User not found")
                
                call.respond(UserProfile(
                    userId = user.id.value.toString(),
                    email = user.email,
                    createdAt = user.createdAt.toString()
                ))
            }
            
            post("/logout") {
                val request = call.receive<LogoutRequest>()
                tokenService.revokeRefreshToken(request.refreshToken)
                call.respond(OkResponse())
            }
        }
    }
}
