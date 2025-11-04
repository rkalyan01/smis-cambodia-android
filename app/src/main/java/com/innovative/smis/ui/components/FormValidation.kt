package com.innovative.smis.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.innovative.smis.R
import java.util.regex.Pattern

object FormValidation {
    
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    @Composable
    fun validateRequired(value: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_required)
            )
        } else {
            ValidationResult(isValid = true)
        }
    }

    @Composable
    fun validatePhoneNumber(value: String, isRequired: Boolean = false): ValidationResult {
        if (value.isBlank() && !isRequired) {
            return ValidationResult(isValid = true)
        }
        
        if (value.isBlank() && isRequired) {
            return ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_required)
            )
        }
        
        val phonePattern = Pattern.compile("^[+]?[0-9]{8,15}$")
        return if (phonePattern.matcher(value).matches()) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_invalid_phone)
            )
        }
    }

    /**
     * Validates Cambodian phone numbers
     * 
     * Valid formats:
     * - Domestic: 0XX XXX XXX(X) → 9-10 digits (starts with 0)
     * - International: +855 XX XXX XXX(X) or 855 XX XXX XXX(X) → 12-13 characters
     * 
     * Mobile prefixes: 10-18, 31, 38, 60-61, 69, 70-71, 77-78, 80-88, 90, 92-93, 95-99
     * Landline prefixes: 2X, 3X (area codes)
     */
    @Composable
    fun validateCambodianPhone(value: String, isRequired: Boolean = false): ValidationResult {
        if (value.isBlank() && !isRequired) {
            return ValidationResult(isValid = true)
        }
        
        if (value.isBlank() && isRequired) {
            return ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_required)
            )
        }
        
        // Remove spaces and dashes for validation
        val cleanedNumber = value.replace(Regex("[\\s-]"), "")
        
        // Domestic format: 0 + (2-digit prefix) + (6-7 digits) = 9-10 digits
        val domesticPattern = Pattern.compile("^0(1[0-8]|2[0-9]|3[0-9]|6[0-179]|7[0-18]|8[0-8]|9[0-38]|69|77|78)\\d{6,7}$")
        
        // International format: +855 or 855 + (2-digit prefix) + (6-7 digits)
        val internationalPattern = Pattern.compile("^(\\+?855)(1[0-8]|2[0-9]|3[0-9]|6[0-179]|7[0-18]|8[0-8]|9[0-38]|69|77|78)\\d{6,7}$")
        
        val isDomesticValid = domesticPattern.matcher(cleanedNumber).matches()
        val isInternationalValid = internationalPattern.matcher(cleanedNumber).matches()
        
        // Check maximum length (prevent excessively long numbers)
        val isTooLong = cleanedNumber.length > 13
        
        return when {
            isTooLong -> ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_phone_too_long)
            )
            isDomesticValid || isInternationalValid -> ValidationResult(isValid = true)
            else -> ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_cambodian_phone_format)
            )
        }
    }

    @Composable
    fun validateEmail(value: String, isRequired: Boolean = false): ValidationResult {
        if (value.isBlank() && !isRequired) {
            return ValidationResult(isValid = true)
        }
        
        if (value.isBlank() && isRequired) {
            return ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_required)
            )
        }
        
        val emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return if (emailPattern.matcher(value).matches()) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_invalid_email)
            )
        }
    }

    @Composable
    fun validateNumber(value: String, isRequired: Boolean = false, min: Double? = null, max: Double? = null): ValidationResult {
        if (value.isBlank() && !isRequired) {
            return ValidationResult(isValid = true)
        }
        
        if (value.isBlank() && isRequired) {
            return ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_required)
            )
        }
        
        val number = value.toDoubleOrNull()
        return when {
            number == null -> ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_invalid_number)
            )
            min != null && number < min -> ValidationResult(
                isValid = false,
                errorMessage = "Value must be at least $min"
            )
            max != null && number > max -> ValidationResult(
                isValid = false,
                errorMessage = "Value must be at most $max"
            )
            else -> ValidationResult(isValid = true)
        }
    }

    @Composable
    fun validateLength(value: String, minLength: Int? = null, maxLength: Int? = null, isRequired: Boolean = false): ValidationResult {
        if (value.isBlank() && !isRequired) {
            return ValidationResult(isValid = true)
        }
        
        if (value.isBlank() && isRequired) {
            return ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_required)
            )
        }
        
        return when {
            minLength != null && value.length < minLength -> ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_min_length, minLength)
            )
            maxLength != null && value.length > maxLength -> ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_max_length, maxLength)
            )
            else -> ValidationResult(isValid = true)
        }
    }

    @Composable
    fun validateDate(value: String, isRequired: Boolean = false): ValidationResult {
        if (value.isBlank() && !isRequired) {
            return ValidationResult(isValid = true)
        }
        
        if (value.isBlank() && isRequired) {
            return ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_required)
            )
        }
        
        val datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")
        return if (datePattern.matcher(value).matches()) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(
                isValid = false,
                errorMessage = stringResource(R.string.validation_invalid_date)
            )
        }
    }
}

data class FormFieldState(
    val value: String = "",
    val isValid: Boolean = true,
    val errorMessage: String? = null,
    val isDirty: Boolean = false
) {
    fun updateValue(newValue: String): FormFieldState {
        return copy(value = newValue, isDirty = true)
    }
    
    fun updateValidation(result: FormValidation.ValidationResult): FormFieldState {
        return copy(
            isValid = result.isValid,
            errorMessage = result.errorMessage
        )
    }
}