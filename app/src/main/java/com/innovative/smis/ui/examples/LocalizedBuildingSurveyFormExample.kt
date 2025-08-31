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
fun LocalizedBuildingSurveyFormExample() {
    // Building Details State
    var buildingName by remember { mutableStateOf("") }
    var buildingType by remember { mutableStateOf("") }
    var numberOfFloors by remember { mutableStateOf("") }
    var totalUnits by remember { mutableStateOf("") }
    var occupiedUnits by remember { mutableStateOf("") }
    var constructionYear by remember { mutableStateOf("") }
    var buildingArea by remember { mutableStateOf("") }
    var compoundArea by remember { mutableStateOf("") }

    // Sanitation System State
    var toiletType by remember { mutableStateOf("") }
    var tankLocation by remember { mutableStateOf("") }
    var tankMaterial by remember { mutableStateOf("") }
    var tankCondition by remember { mutableStateOf("") }
    var lastEmptied by remember { mutableStateOf("") }
    var emptyingFrequency by remember { mutableStateOf("") }
    var accessRoad by remember { mutableStateOf("") }
    var roadWidth by remember { mutableStateOf("") }
    var distanceToTank by remember { mutableStateOf("") }

    // Contact Information State
    var ownerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var alternatePhone by remember { mutableStateOf("") }

    // Validation States
    var buildingNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Building Details Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(stringResource(R.string.form_building_details))

                LocalizedTextField(
                    value = buildingName,
                    onValueChange = {
                        buildingName = it
                        val validation = com.innovative.smis.util.validation.FormValidation.validateRequired(it)
                        buildingNameError = if (!validation.isValid) validation.errorMessage else null
                    },
                    labelResId = R.string.form_building_name,
                    leadingIcon = Icons.Default.Business,
                    isRequired = true,
                    isError = buildingNameError != null,
                    errorMessage = buildingNameError
                )

                Spacer(modifier = Modifier.height(12.dp))

                LocalizedDropdownField(
                    value = buildingType,
                    onValueChange = { buildingType = it },
                    labelResId = R.string.form_building_type,
                    options = FormFieldMapper.buildingTypeOptions,
                    isRequired = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    LocalizedTextField(
                        value = numberOfFloors,
                        onValueChange = { numberOfFloors = it },
                        labelResId = R.string.form_number_of_floors,
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Default.Layers,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LocalizedTextField(
                        value = totalUnits,
                        onValueChange = { totalUnits = it },
                        labelResId = R.string.form_total_units,
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Default.Home,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    LocalizedTextField(
                        value = occupiedUnits,
                        onValueChange = { occupiedUnits = it },
                        labelResId = R.string.form_occupied_units,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LocalizedTextField(
                        value = constructionYear,
                        onValueChange = { constructionYear = it },
                        labelResId = R.string.form_construction_year,
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    LocalizedTextField(
                        value = buildingArea,
                        onValueChange = { buildingArea = it },
                        labelResId = R.string.form_building_area,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LocalizedTextField(
                        value = compoundArea,
                        onValueChange = { compoundArea = it },
                        labelResId = R.string.form_compound_area,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sanitation System Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(stringResource(R.string.form_sanitation_system))

                LocalizedTextField(
                    value = toiletType,
                    onValueChange = { toiletType = it },
                    labelResId = R.string.form_toilet_type,
                    leadingIcon = Icons.Default.Wc
                )

                Spacer(modifier = Modifier.height(12.dp))

                LocalizedTextField(
                    value = tankLocation,
                    onValueChange = { tankLocation = it },
                    labelResId = R.string.form_tank_location,
                    leadingIcon = Icons.Default.LocationOn
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    LocalizedTextField(
                        value = tankMaterial,
                        onValueChange = { tankMaterial = it },
                        labelResId = R.string.form_tank_material,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LocalizedDropdownField(
                        value = tankCondition,
                        onValueChange = { tankCondition = it },
                        labelResId = R.string.form_tank_condition,
                        options = FormFieldMapper.qualityLevelOptions,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    LocalizedTextField(
                        value = lastEmptied,
                        onValueChange = { lastEmptied = it },
                        labelResId = R.string.form_last_emptied,
                        leadingIcon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LocalizedTextField(
                        value = emptyingFrequency,
                        onValueChange = { emptyingFrequency = it },
                        labelResId = R.string.form_emptying_frequency,
                        leadingIcon = Icons.Default.Repeat,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LocalizedTextField(
                    value = accessRoad,
                    onValueChange = { accessRoad = it },
                    labelResId = R.string.form_access_road,
                    leadingIcon = Icons.Default.Route
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    LocalizedTextField(
                        value = roadWidth,
                        onValueChange = { roadWidth = it },
                        labelResId = R.string.form_road_width,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LocalizedTextField(
                        value = distanceToTank,
                        onValueChange = { distanceToTank = it },
                        labelResId = R.string.form_distance_to_tank,
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Default.Straighten,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Contact Information Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(stringResource(R.string.form_contact_info))

                LocalizedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    labelResId = R.string.form_owner_name,
                    leadingIcon = Icons.Default.Person,
                    isRequired = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                LocalizedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        val validation = com.innovative.smis.util.validation.FormValidation.validatePhoneNumber(it, isRequired = true)
                        phoneError = if (!validation.isValid) validation.errorMessage else null
                    },
                    labelResId = R.string.form_phone_number,
                    leadingIcon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone,
                    isRequired = true,
                    isError = phoneError != null,
                    errorMessage = phoneError
                )

                Spacer(modifier = Modifier.height(12.dp))

                LocalizedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        val validation = com.innovative.smis.util.validation.FormValidation.validateEmail(it, isRequired = false)
                        emailError = if (!validation.isValid) validation.errorMessage else null
                    },
                    labelResId = R.string.form_email,
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    isError = emailError != null,
                    errorMessage = emailError
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    LocalizedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        labelResId = R.string.form_contact_person,
                        leadingIcon = Icons.Default.ContactPhone,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LocalizedTextField(
                        value = alternatePhone,
                        onValueChange = { alternatePhone = it },
                        labelResId = R.string.form_alternate_phone,
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Clear form */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.form_clear))
                    }

                    Button(
                        onClick = { /* Save draft */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.form_save_draft))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { /* Submit form */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.form_submit))
                }
            }
        }
    }
}