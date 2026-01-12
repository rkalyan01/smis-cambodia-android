package com.innovative.smis.ui.components

import com.innovative.smis.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    var selectedTypeKey by remember { mutableStateOf("Postpone") }
    var selectedReasonKey by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var postponeTo by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateFormatManager.getApiFormatter()
    val displayFormatter = DateFormatManager.getDisplayFormatter(context)

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

    // Dynamic Labels based on default selection
    val fromLabel = when (selectedTypeKey) {
        "Prepone" -> stringResource(R.string.label_prepone_from)
        "Postpone" -> stringResource(R.string.label_postpone_from)
        else -> stringResource(R.string.label_postpone_from)
    }

    val untilLabel = when (selectedTypeKey) {
        "Prepone" -> stringResource(R.string.label_prepone_to)
        "Postpone" -> stringResource(R.string.label_postpone_to)
        else -> stringResource(R.string.label_postpone_to)
    }

    data class Option(val apiValue: String, val displayLabel: String)

    val typeOptions = listOf(
        Option(apiValue = "Prepone", displayLabel = stringResource(R.string.label_prepone)),
        Option(apiValue = "Postpone", displayLabel = stringResource(R.string.label_postpone))
    )
    val selectedTypeDisplay = typeOptions.find { it.apiValue == selectedTypeKey }?.displayLabel ?: ""

    val reasonOptions = listOf(
        Option(apiValue = "Rescheduled due to ETO", displayLabel = stringResource(R.string.postpone_reason_eto)),
        Option(apiValue = "Rescheduled due to Customers", displayLabel = stringResource(R.string.postpone_reason_customer))
    )
    val selectedReasonDisplay = reasonOptions.find { it.apiValue == selectedReasonKey }?.displayLabel ?: ""

    val whiteBackgroundColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        errorContainerColor = Color.White
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                        text = "Reschedule",
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

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Type Dropdown
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedTypeDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = whiteBackgroundColors
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        typeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayLabel) },
                                onClick = {
                                    selectedTypeKey = option.apiValue
                                    // Reset date when type changes to avoid invalid dates
                                    postponeTo = ""
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Current Date
                OutlinedTextField(
                    value = displayCurrentDate,
                    onValueChange = {},
                    label = { Text(fromLabel) },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = whiteBackgroundColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Reason Dropdown
                var reasonExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = reasonExpanded,
                    onExpandedChange = { reasonExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedReasonDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_reason) + " *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = whiteBackgroundColors
                    )
                    ExposedDropdownMenu(
                        expanded = reasonExpanded,
                        onDismissRequest = { reasonExpanded = false }
                    ) {
                        reasonOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayLabel) },
                                onClick = {
                                    selectedReasonKey = option.apiValue
                                    reasonExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Remark
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text(stringResource(R.string.label_remark)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = whiteBackgroundColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. New Date
                OutlinedTextField(
                    value = if (postponeTo.isNotBlank()) {
                        try {
                            displayFormatter.format(dateFormatter.parse(postponeTo)!!)
                        } catch (e: Exception) {
                            postponeTo
                        }
                    } else "",
                    onValueChange = {},
                    label = { Text(untilLabel) },
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
                    colors = whiteBackgroundColors
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
                            if (selectedTypeKey.isNotBlank() && selectedReasonKey.isNotBlank() && postponeTo.isNotBlank()) {
                                onPostpone(
                                    PostponeData(
                                        postponeType = selectedTypeKey,
                                        postponeFrom = currentDate ?: "",
                                        postponeTo = postponeTo,
                                        reason = selectedReasonKey,
                                        remark = remark
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && selectedTypeKey.isNotBlank() && selectedReasonKey.isNotBlank() && postponeTo.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Reschedule")
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        // 1. Get "Today" at UTC Midnight (Lower bound for everything)
        val todayUtc = remember {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        // 2. Parse "Current Scheduled Date" as UTC Midnight
        val scheduledUtcMillis = remember(currentDate) {
            if (!currentDate.isNullOrBlank()) {
                try {
                    // Parse strict YYYY-MM-DD in UTC
                    val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    utcFormat.parse(currentDate)?.time
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        // 3. Define Logic
        val selectableDates = remember(selectedTypeKey, scheduledUtcMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Safety check
                    if (scheduledUtcMillis == null) return utcTimeMillis >= todayUtc.timeInMillis

                    return if (selectedTypeKey == "Prepone") {
                        // PREPONE: [Today, Scheduled)
                        // Example: Today=18, Scheduled=24. Allows: 18, 19, 20, 21, 22, 23.
                        utcTimeMillis >= todayUtc.timeInMillis && utcTimeMillis < scheduledUtcMillis
                    } else {
                        // POSTPONE: (Scheduled, Infinity)
                        // Example: Scheduled=24. Allows: 25, 26, 27...
                        utcTimeMillis > scheduledUtcMillis
                    }
                }
            }
        }

        // 4. Force state recreation when Type changes to apply new bounds immediately
        key(selectedTypeKey) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = if (selectedTypeKey == "Prepone") {
                    // For Prepone, default to Today (if available)
                    todayUtc.timeInMillis
                } else {
                    // For Postpone, default to the day AFTER scheduled date
                    scheduledUtcMillis?.plus(86400000) ?: (todayUtc.timeInMillis + 86400000)
                },
                selectableDates = selectableDates
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Format back to YYYY-MM-DD
                            // We must use UTC formatter here too, otherwise the selected UTC noon
                            // might shift to the previous day in local time.
                            val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            postponeTo = utcFormat.format(Date(millis))
                        }
                        showDatePicker = false
                    }) { Text(stringResource(R.string.action_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

data class PostponeData(
    val postponeType: String,
    val postponeFrom: String,
    val postponeTo: String,
    val reason: String,
    val remark: String
)