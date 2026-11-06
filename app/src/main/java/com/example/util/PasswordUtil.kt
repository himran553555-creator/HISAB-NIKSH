package com.example.util

data class PasswordStrength(
    val score: Int, // 0 (empty), 1 (weak), 2 (fair), 3 (good), 4 (strong)
    val label: String,
    val hasMinLength: Boolean, // >= 8 characters
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean
)

object PasswordUtil {
    fun evaluate(password: String): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength(
                score = 0,
                label = "Empty",
                hasMinLength = false,
                hasUppercase = false,
                hasLowercase = false,
                hasDigit = false,
                hasSpecialChar = false
            )
        }

        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        var points = 0
        if (password.length >= 6) points++
        if (hasMinLength) points++
        if (hasUppercase && hasLowercase) points++
        if (hasDigit) points++
        if (hasSpecialChar) points++

        val score = when {
            points <= 1 -> 1
            points in 2..3 -> 2
            points == 4 -> 3
            else -> 4
        }

        val label = when (score) {
            1 -> "Weak"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Strong"
            else -> "Weak"
        }

        return PasswordStrength(
            score = score,
            label = label,
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecialChar = hasSpecialChar
        )
    }
}
