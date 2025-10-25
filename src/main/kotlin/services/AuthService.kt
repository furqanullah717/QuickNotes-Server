package com.codewithfk.services

import com.codewithfk.domain.models.User
import com.codewithfk.domain.models.Users
import com.codewithfk.util.Hashing
import com.codewithfk.util.Time
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import java.util.*

class AuthService {
    
    fun signup(email: String, password: String): User {
        return transaction {
            val existingUser = User.find { Users.email eq email.lowercase() }.firstOrNull()
            if (existingUser != null) {
                throw IllegalArgumentException("User with email $email already exists")
            }
            if (password.length < 4) {
                throw IllegalArgumentException("Password must be at least 8 characters long")
            }
            User.new {
                this.email = email.lowercase()
                this.passwordHash = Hashing.hashPassword(password)
                this.createdAt = Time.now()
                this.updatedAt = Time.now()
            }
        }
    }
    
    fun login(email: String, password: String): User {
        return transaction {
            val user = User.find { Users.email eq email.lowercase() }.firstOrNull()
                ?: throw IllegalArgumentException("Invalid email or password")
            
            if (!Hashing.verifyPassword(password, user.passwordHash)) {
                throw IllegalArgumentException("Invalid email or password")
            }
            
            user
        }
    }
    
    fun getUserById(userId: UUID): User? {
        return transaction {
            User.findById(userId)
        }
    }
    
    fun getUserByEmail(email: String): User? {
        return transaction {
            User.find { Users.email eq email.lowercase() }.firstOrNull()
        }
    }
    
    fun updatePassword(userId: UUID, newPassword: String) {
        transaction {
            val user = User.findById(userId) ?: throw IllegalArgumentException("User not found")
            user.passwordHash = Hashing.hashPassword(newPassword)
            user.updatedAt = Time.now()
        }
    }
}
