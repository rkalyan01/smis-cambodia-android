package com.innovative.smis.util.validation

object FormValidation {
    
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )
    
     fun validateRequired(value: String, fieldName: String = "Field"): ValidationResult {
        return if (value.trim().isEmpty()) {
            ValidationResult(false, "$fieldName is required")
        } else {
            ValidationResult(true)
        }
    }
    
    fun validatePhoneNumber(value: String, isRequired: Boolean = false): ValidationResult {
        if (!isRequired && value.trim().isEmpty()) {
            return ValidationResult(true)
        }
        
        if (value.trim().isEmpty() && isRequired) {
            return ValidationResult(false, "Phone number is required")
        }
        
        val phoneRegex = "^[+]?[0-9\\s\\-\\(\\)]{8,15}$".toRegex()
        
        return if (phoneRegex.matches(value.trim())) {
            ValidationResult(true)
        } else {
            ValidationResult(false, "Invalid phone number format")
        }
    }
    
    fun validateEmail(value: String, isRequired: Boolean = false): ValidationResult {
        if (!isRequired && value.trim().isEmpty()) {
            return ValidationResult(true)
        }
        
        if (value.trim().isEmpty() && isRequired) {
            return ValidationResult(false, "Email is required")
        }
        
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        
        return if (emailRegex.matches(value.trim())) {
            ValidationResult(true)
        } else {
            ValidationResult(false, "Invalid email format")
        }
    }

    fun validateNumeric(value: String, isRequired: Boolean = false, min: Int? = null, max: Int? = null): ValidationResult {
        if (!isRequired && value.trim().isEmpty()) {
            return ValidationResult(true)
        }
        
        if (value.trim().isEmpty() && isRequired) {
            return ValidationResult(false, "This field is required")
        }
        
        val numericValue = value.trim().toIntOrNull()
        if (numericValue == null) {
            return ValidationResult(false, "Must be a valid number")
        }
        
        if (min != null && numericValue < min) {
            return ValidationResult(false, "Must be at least $min")
        }
        
        if (max != null && numericValue > max) {
            return ValidationResult(false, "Must be at most $max")
        }
        
        return ValidationResult(true)
    }

    fun validateYear(value: String, isRequired: Boolean = false): ValidationResult {
        if (!isRequired && value.trim().isEmpty()) {
            return ValidationResult(true)
        }
        
        if (value.trim().isEmpty() && isRequired) {
            return ValidationResult(false, "Year is required")
        }
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val year = value.trim().toIntOrNull()
        
        if (year == null || year < 1900 || year > currentYear + 5) {
            return ValidationResult(false, "Invalid year (1900-${currentYear + 5})")
        }
        
        return ValidationResult(true)
    }
}