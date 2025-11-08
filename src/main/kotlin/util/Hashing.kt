package com.codewithfk.util

import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom

object Hashing {
    private val secureRandom = SecureRandom()
    
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
    
    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }
    
    fun generateRandomToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun generateResetCode(): String {
        val bytes = ByteArray(3)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }.uppercase().take(6)
    }
    
    fun hashToken(token: String): String {
        return BCrypt.hashpw(token, BCrypt.gensalt())
    }
    
    fun verifyToken(token: String, hash: String): Boolean {
        return BCrypt.checkpw(token, hash)
    }
}

