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