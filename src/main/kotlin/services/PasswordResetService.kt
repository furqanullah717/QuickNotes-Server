package com.codewithfk.services

import com.codewithfk.domain.models.PasswordReset
import com.codewithfk.domain.models.PasswordResets
import com.codewithfk.domain.models.User
import com.codewithfk.domain.models.Users
import com.codewithfk.util.Hashing
import com.codewithfk.util.Time
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.util.*

class PasswordResetService {
    
    fun generateResetCode(userId: UUID): String {
        val code = Hashing.generateResetCode()
        val codeHash = Hashing.hashToken(code)
        val expiresAt = Time.now().plusSeconds(15 * 60L) // 15 minutes
        
        transaction {
            PasswordResets.deleteWhere { PasswordResets.userId eq userId }

            PasswordReset.new {
                this.userId = EntityID(userId, Users)
                this.codeHash = codeHash
                this.expiresAt = expiresAt
                this.used = false
                this.createdAt = Time.now()
            }
        }
        
        return code
    }
    
    fun validateResetCode(email: String, code: String): User? {
        val codeHash = Hashing.hashToken(code)
        
        return transaction {
            val user = User.find { Users.email eq email.lowercase() }.firstOrNull()
                ?: return@transaction null
            
            val passwordReset = PasswordReset.find { 
                PasswordResets.userId eq user.id.value and 
                (PasswordResets.codeHash eq codeHash) and
                (PasswordResets.used eq false)
            }.firstOrNull()
            
            if (passwordReset == null || passwordReset.expiresAt.isBefore(Time.now())) {
                null
            } else {
                user
            }
        }
    }
    
    fun markResetCodeUsed(email: String, code: String) {
        val codeHash = Hashing.hashToken(code)
        
        transaction {
            val user = User.find { Users.email eq email.lowercase() }.firstOrNull()
                ?: return@transaction
            
            PasswordReset.find { 
                PasswordResets.userId eq user.id.value and 
                (PasswordResets.codeHash eq codeHash)
            }.forEach { it.used = true }
        }
    }
    
    fun cleanupExpiredCodes() {
        transaction {
            PasswordResets.deleteWhere { PasswordResets.expiresAt less Time.now() }
        }
    }
}
