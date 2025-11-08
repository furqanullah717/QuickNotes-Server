package com.codewithfk.routes

import com.codewithfk.domain.dto.AccountDeletionRequest
import com.codewithfk.domain.dto.AccountDeletionStatusResponse
import com.codewithfk.domain.dto.OkResponse
import com.codewithfk.services.AccountDeletionService
import com.codewithfk.services.AuthService
import com.codewithfk.util.ProblemFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Duration
import java.util.*

fun Route.accountDeletionRoutes() {
    val accountDeletionService = AccountDeletionService()
    val authService = AuthService()
    
    // HTML page for account deletion request
    get("/delete-account") {
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Delete Account - QuickerNotes</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .container {
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                        max-width: 500px;
                        width: 100%;
                        padding: 40px;
                    }
                    h1 {
                        color: #333;
                        margin-bottom: 10px;
                        font-size: 28px;
                    }
                    .subtitle {
                        color: #666;
                        margin-bottom: 30px;
                        font-size: 14px;
                        line-height: 1.6;
                    }
                    .warning {
                        background: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin-bottom: 25px;
                        border-radius: 4px;
                    }
                    .warning strong {
                        color: #856404;
                        display: block;
                        margin-bottom: 8px;
                    }
                    .warning p {
                        color: #856404;
                        font-size: 14px;
                        line-height: 1.5;
                    }
                    .form-group {
                        margin-bottom: 20px;
                    }
                    label {
                        display: block;
                        margin-bottom: 8px;
                        color: #333;
                        font-weight: 500;
                        font-size: 14px;
                    }
                    input {
                        width: 100%;
                        padding: 12px;
                        border: 2px solid #e0e0e0;
                        border-radius: 6px;
                        font-size: 16px;
                        transition: border-color 0.3s;
                    }
                    input:focus {
                        outline: none;
                        border-color: #667eea;
                    }
                    button {
                        width: 100%;
                        padding: 14px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        border: none;
                        border-radius: 6px;
                        font-size: 16px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: transform 0.2s, box-shadow 0.2s;
                    }
                    button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
                    }
                    button:active {
                        transform: translateY(0);
                    }
                    button:disabled {
                        opacity: 0.6;
                        cursor: not-allowed;
                        transform: none;
                    }
                    .message {
                        margin-top: 20px;
                        padding: 12px;
                        border-radius: 6px;
                        font-size: 14px;
                        display: none;
                    }
                    .message.success {
                        background: #d4edda;
                        color: #155724;
                        border: 1px solid #c3e6cb;
                        display: block;
                    }
                    .message.error {
                        background: #f8d7da;
                        color: #721c24;
                        border: 1px solid #f5c6cb;
                        display: block;
                    }
                    .info {
                        margin-top: 20px;
                        padding: 15px;
                        background: #e7f3ff;
                        border-left: 4px solid #2196F3;
                        border-radius: 4px;
                        font-size: 13px;
                        color: #0c5460;
                        line-height: 1.6;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Delete My Account</h1>
                    <p class="subtitle">Request permanent deletion of your account and all associated data</p>
                    
                    <div class="warning">
                        <strong>⚠️ Important Notice</strong>
                        <p>Your account and all data will be permanently deleted after 90 days. This action cannot be undone. You can cancel this request anytime before the deletion date by logging in.</p>
                    </div>
                    
                    <form id="deletionForm">
                        <div class="form-group">
                            <label for="email">Email Address</label>
                            <input type="email" id="email" name="email" required autocomplete="email">
                        </div>
                        
                        <div class="form-group">
                            <label for="password">Password</label>
                            <input type="password" id="password" name="password" required autocomplete="current-password">
                        </div>
                        
                        <button type="submit" id="submitBtn">Request Account Deletion</button>
                    </form>
                    
                    <div id="message" class="message"></div>
                    
                    <div class="info">
                        <strong>What happens next?</strong><br>
                        • Your deletion request will be processed immediately<br>
                        • Your account will be scheduled for deletion in 90 days<br>
                        • You'll receive a confirmation email<br>
                        • You can cancel this request by logging in before the deletion date
                    </div>
                </div>
                
                <script>
                    document.getElementById('deletionForm').addEventListener('submit', async (e) => {
                        e.preventDefault();
                        
                        const submitBtn = document.getElementById('submitBtn');
                        const messageDiv = document.getElementById('message');
                        const email = document.getElementById('email').value;
                        const password = document.getElementById('password').value;
                        
                        submitBtn.disabled = true;
                        submitBtn.textContent = 'Processing...';
                        messageDiv.className = 'message';
                        messageDiv.textContent = '';
                        
                        try {
                            const response = await fetch('/api/account/delete', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify({ email, password })
                            });
                            
                            const data = await response.json();
                            
                            if (response.ok) {
                                messageDiv.className = 'message success';
                                messageDiv.textContent = '✓ Account deletion requested successfully. Your account will be deleted in 90 days. You can cancel this request by logging in before then.';
                                document.getElementById('deletionForm').reset();
                            } else {
                                messageDiv.className = 'message error';
                                messageDiv.textContent = data.detail || 'Failed to request account deletion. Please check your email and password.';
                            }
                        } catch (error) {
                            messageDiv.className = 'message error';
                            messageDiv.textContent = 'An error occurred. Please try again later.';
                        } finally {
                            submitBtn.disabled = false;
                            submitBtn.textContent = 'Request Account Deletion';
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
        
        call.respondText(html, ContentType.Text.Html)
    }
    
    // API endpoint for account deletion request
    route("/api/account") {
        post("/delete") {
            try {
                val request = call.receive<AccountDeletionRequest>()
                val success = accountDeletionService.requestAccountDeletion(request.email, request.password)
                
                if (success) {
                    call.respond(OkResponse(ok = true))
                } else {
                    val problem = ProblemFactory.unauthorized("Invalid email or password")
                    call.respond(HttpStatusCode.Unauthorized, problem)
                }
            } catch (e: Exception) {
                val problem = ProblemFactory.badRequest(e.message ?: "Invalid request")
                call.respond(HttpStatusCode.BadRequest, problem)
            }
        }
        
        authenticate("auth-jwt") {
            get("/deletion-status") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("sub")?.asString() ?: "")
                
                val deletion = accountDeletionService.getDeletionStatus(userId)
                
                if (deletion != null && deletion.deletedAt == null) {
                    val daysRemaining = Duration.between(
                        java.time.Instant.now(),
                        deletion.scheduledDeletionAt
                    ).toDays().toInt()
                    
                    call.respond(AccountDeletionStatusResponse(
                        requested = true,
                        requestedAt = deletion.requestedAt.toString(),
                        scheduledDeletionAt = deletion.scheduledDeletionAt.toString(),
                        daysRemaining = if (daysRemaining > 0) daysRemaining else 0
                    ))
                } else {
                    call.respond(AccountDeletionStatusResponse(requested = false))
                }
            }
            
            post("/cancel-deletion") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("sub")?.asString() ?: "")
                
                val success = accountDeletionService.cancelAccountDeletion(userId)
                
                if (success) {
                    call.respond(OkResponse(ok = true))
                } else {
                    val problem = ProblemFactory.badRequest("No active deletion request found")
                    call.respond(HttpStatusCode.BadRequest, problem)
                }
            }
        }
    }
}

