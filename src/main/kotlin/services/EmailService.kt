package com.codewithfk.services

import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.*

class EmailService(
    private val smtpHost: String,
    private val smtpPort: Int,
    private val smtpUser: String?,
    private val smtpPassword: String?,
    private val fromEmail: String
) {
    
    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort.toString())
            put("mail.smtp.auth", if (smtpUser != null) "true" else "false")
            put("mail.smtp.starttls.enable", "true")
        }
        
        if (smtpUser != null && smtpPassword != null) {
            Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(smtpUser, smtpPassword)
                }
            })
        } else {
            Session.getInstance(props)
        }
    }
    
    fun sendPasswordResetEmail(toEmail: String, resetCode: String) {
        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail))
                setRecipients(Message.RecipientType.TO, toEmail)
                subject = "Password Reset - QuickerNotes"
                
                val body = """
                    Hi,
                    
                    You requested a password reset for your QuickerNotes account.
                    
                    Your reset code is: $resetCode
                    
                    This code will expire in 15 minutes.
                    
                    If you didn't request this reset, please ignore this email.
                    
                    Best regards,
                    QuickerNotes Team
                """.trimIndent()
                
                setText(body)
            }
            
            Transport.send(message)
        } catch (e: Exception) {
            throw RuntimeException("Failed to send email: ${e.message}", e)
        }
    }
    
    fun sendWelcomeEmail(toEmail: String) {
        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail))
                setRecipients(Message.RecipientType.TO, toEmail)
                subject = "Welcome to QuickerNotes!"
                
                val body = """
                    Hi,
                    
                    Welcome to QuickerNotes! Your account has been successfully created.
                    
                    You can now start taking notes and syncing them across all your devices.
                    
                    Best regards,
                    QuickerNotes Team
                """.trimIndent()
                
                setText(body)
            }
            
            Transport.send(message)
        } catch (e: Exception) {
            println("Failed to send welcome email: ${e.message}")
        }
    }
}
