package com.taytek.basehw.domain.util

object AuthValidator {
    
    // Password criteria: min 8 characters, at least one uppercase, one lowercase, and one digit
    fun isPasswordValid(password: String): Boolean {
        if (password.length < 8) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUpper && hasLower && hasDigit
    }

    // Basic profanity filter for usernames (common bad words)
    // In a real app, this would be a more comprehensive list or an API call.
    private val badWords = listOf("fuck", "shit", "asshole", "bitch", "piss", "slut", "cunt", "küfür", "aptal", "salak")

    fun isUsernameClean(username: String): Boolean {
        val lower = username.lowercase()
        return badWords.none { lower.contains(it) }
    }

    fun isUsernameFormatValid(username: String): Boolean {
        // Alphanumeric, underscores, 3-8 chars
        val regex = "^[a-zA-Z0-9_]{3,8}$".toRegex()
        return regex.matches(username)
    }
}
