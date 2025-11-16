package com.innovative.smis.util.validation

/**
 * Input validators for form fields with specific format requirements
 */
object InputValidators {
    
    /**
     * Validates and formats Size of Storage Tank input
     * Format: 3 digits before decimal, 2 digits after decimal (e.g., 999.99)
     */
    fun validateStorageTankSize(input: String): String {
        // Remove all non-digit and non-decimal characters
        val cleaned = input.filter { it.isDigit() || it == '.' }
        
        // Split by decimal point
        val parts = cleaned.split(".")
        
        return when {
            parts.size == 1 -> {
                // No decimal point yet - limit to 3 digits
                parts[0].take(3)
            }
            parts.size == 2 -> {
                // Has decimal point - limit to 3 digits before, 2 after
                val beforeDecimal = parts[0].take(3)
                val afterDecimal = parts[1].take(2)
                "$beforeDecimal.$afterDecimal"
            }
            else -> {
                // Multiple decimal points - take only first two parts
                val beforeDecimal = parts[0].take(3)
                val afterDecimal = parts[1].take(2)
                "$beforeDecimal.$afterDecimal"
            }
        }
    }
    
    /**
     * Validates and formats Construction Year input
     * Format: 4 digits only, no decimal (e.g., 2024)
     */
    fun validateConstructionYear(input: String): String {
        // Remove all non-digit characters and limit to 4 digits
        return input.filter { it.isDigit() }.take(4)
    }
    
    /**
     * Validates and formats Amount of Extra Payment input
     * Format: 4 digits before decimal, 2 digits after decimal (e.g., 9999.99)
     */
    fun validateExtraPaymentAmount(input: String): String {
        // Remove all non-digit and non-decimal characters
        val cleaned = input.filter { it.isDigit() || it == '.' }
        
        // Split by decimal point
        val parts = cleaned.split(".")
        
        return when {
            parts.size == 1 -> {
                // No decimal point yet - limit to 4 digits
                parts[0].take(4)
            }
            parts.size == 2 -> {
                // Has decimal point - limit to 4 digits before, 2 after
                val beforeDecimal = parts[0].take(4)
                val afterDecimal = parts[1].take(2)
                "$beforeDecimal.$afterDecimal"
            }
            else -> {
                // Multiple decimal points - take only first two parts
                val beforeDecimal = parts[0].take(4)
                val afterDecimal = parts[1].take(2)
                "$beforeDecimal.$afterDecimal"
            }
        }
    }
}
