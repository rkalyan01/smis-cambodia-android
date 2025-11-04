package com.innovative.smis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.innovative.smis.R
import java.util.Calendar

/**
 * Modern Material 3 DatePicker with DateFormatManager integration
 * This is the standardized date picker component used across all screens
 * 
 * @param label The field label
 * @param selectedDate The selected date in milliseconds (null if no date selected)
 * @param onDateSelected Callback when date is selected (receives milliseconds or null)
 * @param isFutureDateAllowed If true, allows today and future dates only. If false, allows today and past dates only.
 * @param enabled Whether the field is enabled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: Long?,
    onDateSelected: (Long?) -> Unit,
    isFutureDateAllowed: Boolean = true,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val dateText = selectedDate?.let { 
        com.innovative.smis.util.helper.DateFormatManager.formatTimestampForDisplay(context, it)
    } ?: ""

    OutlinedTextField(
        value = dateText,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(
                onClick = { showDatePicker = true },
                enabled = enabled
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.cd_select_date))
            }
        },
        colors = if (!enabled) {
            OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            OutlinedTextFieldDefaults.colors()
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            selectableDates = remember(isFutureDateAllowed) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        return if (isFutureDateAllowed) {
                            utcTimeMillis >= today  // Allow today and future dates
                        } else {
                            utcTimeMillis <= today  // Allow today and past dates only
                        }
                    }
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            modifier = Modifier.fillMaxWidth(0.95f),
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
