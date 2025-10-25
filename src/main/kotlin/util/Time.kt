package com.codewithfk.util

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object Time {
    private val iso8601Formatter = DateTimeFormatter.ISO_INSTANT
    
    fun now(): Instant = Instant.now()
    
    fun format(instant: Instant): String = iso8601Formatter.format(instant)
    
    fun parse(iso8601String: String): Instant? {
        return try {
            Instant.parse(iso8601String)
        } catch (e: DateTimeParseException) {
            null
        }
    }
    
    fun isValidIso8601(iso8601String: String): Boolean {
        return parse(iso8601String) != null
    }
}
