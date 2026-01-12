package com.innovative.smis.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ValidatedPhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String? = null,
    enabled: Boolean = true,
    isRequired: Boolean = false
) {
    if (enabled) {
        // GAP FIXED: The error is now passed INTO the component
        PhoneNumberField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier,
            enabled = true,
            isRequired = isRequired,
            errorMessage = errorMessage // Pass it here
        )
    } else {
        // Disabled State
        ValidatedTextField(
            value = value,
            onValueChange = {},
            label = label,
            errorMessage = errorMessage,
            enabled = false,
            isRequired = isRequired
        )
    }
}