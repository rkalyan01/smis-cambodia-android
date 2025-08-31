package com.innovative.smis.util

data class FormValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap()
)