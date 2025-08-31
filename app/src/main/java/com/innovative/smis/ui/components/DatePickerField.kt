package com.innovative.smis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    maxDate: LocalDate? = null,
    minDate: LocalDate? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedDate,
            onValueChange = { },
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select Date",
                    modifier = Modifier.clickable { 
                        if (enabled) showDatePicker = true 
                    }
                )
            },
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    if (enabled) showDatePicker = true 
                }
        )
        
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = if (selectedDate.isNotBlank()) {
                    try {
                        LocalDate.parse(selectedDate, dateFormat)
                            .atStartOfDay()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    } catch (e: Exception) {
                        null
                    }
                } else null,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val date = java.time.Instant
                            .ofEpochMilli(utcTimeMillis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        
                        val isAfterMin = minDate?.let { date >= it } ?: true
                        val isBeforeMax = maxDate?.let { date <= it } ?: true
                        
                        return isAfterMin && isBeforeMax
                    }
                }
            )
            
            DatePickerDialog(
                onDateSelected = { selectedDateMillis ->
                    selectedDateMillis?.let {
                        val localDate = java.time.Instant
                            .ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(localDate.format(dateFormat))
                    }
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false },
                datePickerState = datePickerState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            DatePicker(state = datePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = { onDateSelected(datePickerState.selectedDateMillis) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}