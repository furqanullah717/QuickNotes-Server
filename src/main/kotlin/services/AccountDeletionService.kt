package com.codewithfk.services

import com.codewithfk.domain.models.AccountDeletion
import com.codewithfk.domain.models.AccountDeletions
import com.codewithfk.domain.models.User
import com.codewithfk.domain.models.Users
import com.codewithfk.util.Hashing
import com.codewithfk.util.Time
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.and
import java.util.*

class AccountDeletionService {
    
    fun requestAccountDeletion(email: String, password: String): Boolean {
        return transaction {
            val user = User.find { Users.email eq email.lowercase() }.firstOrNull()
                ?: return@transaction false
            
            // Verify password
            if (!Hashing.verifyPassword(password, user.passwordHash)) {
                return@transaction false
            }
            
            // Check if deletion already requested
            val existingDeletion = AccountDeletion.find { 
                AccountDeletions.userId eq user.id.value 
            }.firstOrNull()
            
            if (existingDeletion != null && existingDeletion.deletedAt == null) {
                // Already requested, return true
                return@transaction true
            }
            
            // Create new deletion request
            val now = Time.now()
            val scheduledDeletionAt = now.plusSeconds(90 * 24 * 60 * 60L) // 90 days
            
            if (existingDeletion != null) {
                // Update existing record
                existingDeletion.requestedAt = now
                existingDeletion.scheduledDeletionAt = scheduledDeletionAt
                existingDeletion.deletedAt = null
            } else {
                // Create new record
                AccountDeletion.new {
                    this.userId = EntityID(user.id.value, Users)
                    this.requestedAt = now
                    this.scheduledDeletionAt = scheduledDeletionAt
                    this.deletedAt = null
                    this.createdAt = now
                }
            }
            
            true
        }
    }
    
    fun cancelAccountDeletion(userId: UUID): Boolean {
        return transaction {
            val deletion = AccountDeletion.find { 
                AccountDeletions.userId eq userId 
            }.firstOrNull()
            
            if (deletion != null && deletion.deletedAt == null) {
                deletion.deletedAt = Time.now() // Mark as cancelled
                true
            } else {
                false
            }
        }
    }
    
    fun getDeletionStatus(userId: UUID): AccountDeletion? {
        return transaction {
            AccountDeletion.find { 
                AccountDeletions.userId eq userId 
            }.firstOrNull()
        }
    }
    
    fun processScheduledDeletions() {
        val now = Time.now()
        
        transaction {
            val deletionsToProcess = AccountDeletion.find {
                (AccountDeletions.scheduledDeletionAt less now) and
                (AccountDeletions.deletedAt.isNull())
            }.toList()
            
            deletionsToProcess.forEach { deletion ->
                try {
                    // Delete user (cascade will handle related data)
                    val user = User.findById(deletion.userId.value)
                    user?.delete()
                    
                    // Mark deletion as processed
                    deletion.deletedAt = now
                } catch (e: Exception) {
                    println("Error deleting user ${deletion.userId.value}: ${e.message}")
                }
            }
        }
    }
    
    fun cleanupOldDeletionRecords() {
        val cutoffDate = Time.now().minusSeconds(30 * 24 * 60 * 60L) // 30 days ago
        
        transaction {
            AccountDeletions.deleteWhere { 
                (AccountDeletions.deletedAt.isNotNull()) and
                (AccountDeletions.deletedAt less cutoffDate)
            }
        }
    }
}

