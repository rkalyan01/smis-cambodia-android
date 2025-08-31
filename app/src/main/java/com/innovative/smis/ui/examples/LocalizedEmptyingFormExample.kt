package com.innovative.smis.ui.examples

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.innovative.smis.R
import com.innovative.smis.ui.components.*
import com.innovative.smis.util.FormFieldMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalizedEmptyingFormExample() {
    var workerName by remember { mutableStateOf("") }
    var supervisorName by remember { mutableStateOf("") }
    var teamSize by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var truckCapacity by remember { mutableStateOf("") }
    var tankType by remember { mutableStateOf("") }
    var tankCapacity by remember { mutableStateOf("") }
    var sludgeLevel by remember { mutableStateOf("") }
    var waterLevel by remember { mutableStateOf("") }
    var volumeEmptied by remember { mutableStateOf("") }
    var sludgeQuality by remember { mutableStateOf("") }
    var weather by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var workerNameError by remember { mutableStateOf<String?>(null) }
    var teamSizeError by remember { mutableStateOf<String?>(null) }
    var tankCapacityError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(stringResource(R.string.form_task_details))

        LocalizedTextField(
            value = workerName,
            onValueChange = {
                workerName = it
                val validation = com.innovative.smis.util.validation.FormValidation.validateRequired(it)
                workerNameError = if (!validation.isValid) validation.errorMessage else null
            },
            labelResId = R.string.form_worker_name,
            leadingIcon = Icons.Default.Person,
            isRequired = true,
            isError = workerNameError != null,
            errorMessage = workerNameError
        )

        LocalizedTextField(
            value = supervisorName,
            onValueChange = { supervisorName = it },
            labelResId = R.string.form_supervisor,
            leadingIcon = Icons.Default.SupervisorAccount
        )

        LocalizedTextField(
            value = teamSize,
            onValueChange = {
                teamSize = it
                val validation = com.innovative.smis.util.validation.FormValidation.validateNumeric(it, isRequired = true, min = 1, max = 20)
                teamSizeError = if (!validation.isValid) validation.errorMessage else null
            },
            labelResId = R.string.form_team_size,
            leadingIcon = Icons.Default.Group,
            keyboardType = KeyboardType.Number,
            isRequired = true,
            isError = teamSizeError != null,
            errorMessage = teamSizeError
        )

        SectionHeader(stringResource(R.string.form_equipment))

        LocalizedTextField(
            value = vehicleNumber,
            onValueChange = { vehicleNumber = it },
            labelResId = R.string.form_vehicle_number,
            leadingIcon = Icons.Default.LocalShipping
        )

        LocalizedTextField(
            value = truckCapacity,
            onValueChange = { truckCapacity = it },
            labelResId = R.string.form_truck_capacity,
            keyboardType = KeyboardType.Number,
            leadingIcon = Icons.Default.LocalGasStation
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(stringResource(R.string.form_emptying_type))

                LocalizedDropdownField(
                    value = tankType,
                    onValueChange = { tankType = it },
                    labelResId = R.string.form_tank_type,
                    options = FormFieldMapper.tankTypeOptions,
                    isRequired = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LocalizedTextField(
                    value = tankCapacity,
                    onValueChange = {
                        tankCapacity = it
                        val validation = com.innovative.smis.util.validation.FormValidation.validateNumeric(it, isRequired = true, min = 100)
                        tankCapacityError = if (!validation.isValid) validation.errorMessage else null
                    },
                    labelResId = R.string.form_tank_capacity,
                    keyboardType = KeyboardType.Number,
                    isRequired = true,
                    isError = tankCapacityError != null,
                    errorMessage = tankCapacityError
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    LocalizedTextField(
                        value = sludgeLevel,
                        onValueChange = { sludgeLevel = it },
                        labelResId = R.string.form_sludge_level,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LocalizedTextField(
                        value = waterLevel,
                        onValueChange = { waterLevel = it },
                        labelResId = R.string.form_water_level,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LocalizedTextField(
                    value = volumeEmptied,
                    onValueChange = { volumeEmptied = it },
                    labelResId = R.string.form_volume_emptied,
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(16.dp))

                LocalizedDropdownField(
                    value = sludgeQuality,
                    onValueChange = { sludgeQuality = it },
                    labelResId = R.string.form_sludge_quality,
                    options = FormFieldMapper.qualityLevelOptions
                )
            }
        }

        SectionHeader(stringResource(R.string.form_environmental_conditions))

        LocalizedTextField(
            value = weather,
            onValueChange = { weather = it },
            labelResId = R.string.form_weather,
            leadingIcon = Icons.Default.Cloud
        )

        LocalizedTextField(
            value = temperature,
            onValueChange = { temperature = it },
            labelResId = R.string.form_temperature,
            keyboardType = KeyboardType.Number,
            leadingIcon = Icons.Default.Thermostat
        )

        SectionHeader(stringResource(R.string.form_notes))

        LocalizedTextField(
            value = notes,
            onValueChange = { notes = it },
            labelResId = R.string.form_additional_comments,
            singleLine = false,
            maxLines = 4,
            modifier = Modifier.height(120.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { /* Save draft action */ },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.form_save_draft))
            }

            Button(
                onClick = { /* Submit action */ },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.form_submit))
            }
        }
    }
}

@Composable
fun validateEmptyingForm(
    workerName: String,
    teamSize: String,
    tankCapacity: String,
    tankType: String
): Boolean {
    val workerNameValid = com.innovative.smis.util.validation.FormValidation.validateRequired(workerName).isValid
    val teamSizeValid = com.innovative.smis.util.validation.FormValidation.validateNumeric(teamSize, isRequired = true, min = 1, max = 20).isValid
    val tankCapacityValid = com.innovative.smis.util.validation.FormValidation.validateNumeric(tankCapacity, isRequired = true, min = 100).isValid
    val tankTypeValid = com.innovative.smis.util.validation.FormValidation.validateRequired(tankType).isValid

    return workerNameValid && teamSizeValid && tankCapacityValid && tankTypeValid
}