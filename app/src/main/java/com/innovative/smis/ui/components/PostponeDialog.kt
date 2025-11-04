package com.innovative.smis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.innovative.smis.R
import com.innovative.smis.util.helper.DateFormatManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostponeDialog(
    applicationId: Int,
    currentDate: String?,
    onDismiss: () -> Unit,
    onPostpone: (PostponeData) -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    var reason by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var postponeUntil by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val dateFormatter = DateFormatManager.getApiFormatter() // API format (yyyy-MM-dd)
    val displayFormatter = DateFormatManager.getDisplayFormatter(context) // Display format based on user preference
    
    // Convert currentDate (API format) to display format for showing to user
    val displayCurrentDate = remember(currentDate) {
        try {
            currentDate?.let {
                val date = dateFormatter.parse(it)
                date?.let { displayFormatter.format(date) } ?: it
            } ?: ""
        } catch (e: Exception) {
            currentDate ?: ""
        }
    }
    
    // Reason options
    val reasonOptions = listOf(
        "Rescheduled due to ETO",
        "Rescheduled due to Customers"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.dialog_postpone_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current Date (read-only) - displayed in user's preferred format
                OutlinedTextField(
                    value = displayCurrentDate,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.label_postpone_from)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = false
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Reason Dropdown
                var reasonExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = reasonExpanded,
                    onExpandedChange = { reasonExpanded = it }
                ) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_reason)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = reasonExpanded,
                        onDismissRequest = { reasonExpanded = false }
                    ) {
                        reasonOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    reason = option
                                    reasonExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Remark
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text(stringResource(R.string.label_remark)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Postpone Until Date
                OutlinedTextField(
                    value = if (postponeUntil.isNotBlank()) {
                        try {
                            displayFormatter.format(dateFormatter.parse(postponeUntil)!!)
                        } catch (e: Exception) {
                            postponeUntil
                        }
                    } else "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.label_postpone_until)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.cd_select_date),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    
                    Button(
                        onClick = {
                            if (reason.isNotBlank() && postponeUntil.isNotBlank()) {
                                onPostpone(
                                    PostponeData(
                                        postponeFrom = currentDate ?: "",
                                        postponeUntil = postponeUntil,
                                        reason = reason,
                                        remark = remark
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && reason.isNotBlank() && postponeUntil.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.button_postpone))
                        }
                    }
                }
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        // Set minimum date to tomorrow (exclude today and past dates)
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= tomorrow.timeInMillis
                }
            }
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        postponeUntil = dateFormatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

data class PostponeData(
    val postponeFrom: String,
    val postponeUntil: String,
    val reason: String,
    val remark: String
)
