package com.innovative.smis.ui.components

import com.innovative.smis.R
import com.innovative.smis.ui.components.FormValidation
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovative.smis.util.helper.PhoneNumberFormatter

/**
 * Appends an asterisk (*) to the label if the field is required
 */
fun labelWithAsterisk(label: String, isRequired: Boolean): String {
    return if (isRequired) "$label *" else label
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextFieldWithError(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isRequired: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(labelWithAsterisk(label, isRequired)) },
        isError = error != null,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        modifier = modifier.fillMaxWidth(),
        supportingText = if (error != null) {
            {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        } else null,
        trailingIcon = if (error != null) {
            {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = stringResource(R.string.cd_error),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRequired: Boolean = false,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    val validationResult = FormValidation.validateCambodianPhone(value, isRequired)

    val hasInternalError = !validationResult.isValid && value.isNotBlank()
    val hasExternalError = errorMessage != null
    val isErrorState = hasInternalError || hasExternalError

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(labelWithAsterisk(label, isRequired)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isErrorState,
        supportingText = if (isErrorState) {
            {
                Text(
                    text = if (hasInternalError) validationResult.errorMessage ?: "" else errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        } else null,
        trailingIcon = {
            if (value.isNotEmpty() && validationResult.isValid) {
                IconButton(
                    onClick = {
                        val formattedNumber = PhoneNumberFormatter.formatForDialing(value)
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedNumber"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Outlined.Call,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (isErrorState) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = stringResource(R.string.cd_error),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && enabled },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = { },
            label = { Text(label) },
            isError = error != null,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.cd_dropdown)
                )
            },
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            supportingText = if (error != null) {
                {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            } else null
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ReadOnlyTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        enabled = false,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun YesNoRadioGroup(
    label: String,
    selectedOption: Boolean?,
    onOptionSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRequired: Boolean = false
) {
    Column(modifier = modifier) {
        // FIXED: Use SemiBold for title
        Text(
            text = labelWithAsterisk(label, isRequired),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.padding(bottom = 4.dp) // Reduced padding
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp) // FIXED: Tighter height
                    .selectable(
                        selected = selectedOption == true,
                        enabled = enabled,
                        role = androidx.compose.ui.semantics.Role.RadioButton,
                        onClick = { if (enabled) onOptionSelected(true) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption == true,
                    onClick = { if (enabled) onOptionSelected(true) },
                    enabled = enabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_yes),
                    fontSize = 14.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp) // FIXED: Tighter height
                    .selectable(
                        selected = selectedOption == false,
                        enabled = enabled,
                        role = androidx.compose.ui.semantics.Role.RadioButton,
                        onClick = { if (enabled) onOptionSelected(false) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption == false,
                    onClick = { if (enabled) onOptionSelected(false) },
                    enabled = enabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_no),
                    fontSize = 14.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
fun RadioGroup(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    RadioButtonGroup(
        title = title,
        options = options,
        selectedValue = selectedOption,
        onValueSelected = onOptionSelected,
        modifier = modifier
    )
}

@Composable
fun RadioButtonGroup(
    title: String,
    options: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .selectable(
                        selected = (selectedValue == option),
                        role = androidx.compose.ui.semantics.Role.RadioButton,
                        onClick = { onValueSelected(option) }
                    )
                    .padding(horizontal = 0.dp), // Removed horizontal padding for alignment
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedValue == option),
                    onClick = { onValueSelected(option) }
                )
                Spacer(modifier = Modifier.width(0.dp))
                Text(
                    text = option,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CheckboxWithLabel(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(40.dp), // FIXED: Consistent height
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onCheckedChange(!checked) }
        )
    }
}

@Composable
fun disabledTextFieldColors() = OutlinedTextFieldDefaults.colors(
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
)

@Composable
fun FormErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuField(
    label: String,
    selectedValue: String,
    selectedKey: String,
    options: Map<String, String>,
    onOptionSelected: (key: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRequired: Boolean = false,
    placeholder: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    val displayValue = options[selectedKey] ?: selectedValue

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && enabled },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = { },
            label = { Text(labelWithAsterisk(label, isRequired)) },
            placeholder = if (placeholder != null && displayValue.isEmpty()) { { Text(placeholder) } } else null,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.cd_dropdown)
                )
            },
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (key, value) ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onOptionSelected(key, value)
                        expanded = false
                    }
                )
            }
        }
    }
}