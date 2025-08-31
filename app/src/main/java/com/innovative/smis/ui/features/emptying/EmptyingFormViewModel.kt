package com.innovative.smis.ui.features.emptying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.response.*
import com.innovative.smis.data.model.EmptyingFormState
import com.innovative.smis.data.model.PurposeOptionData
import com.innovative.smis.data.repository.EmptyingRepository
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.FormValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EmptyingFormViewModel(
    private val emptyingRepository: EmptyingRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(EmptyingFormState())
    val formState: StateFlow<EmptyingFormState> = _formState.asStateFlow()

    private val _submitResult = MutableStateFlow<Resource<EmptyingFormResponse>?>(null)
    val submitResult: StateFlow<Resource<EmptyingFormResponse>?> = _submitResult.asStateFlow()

    fun updateFormState(newState: EmptyingFormState) {
        _formState.value = newState.copy(
            // Clear field-specific errors when user modifies the field
            applicantNameError = if (newState.applicantName != _formState.value.applicantName) null
            else newState.applicantNameError,
            applicantContactError = if (newState.applicantContact != _formState.value.applicantContact) null
            else newState.applicantContactError,
            proposeEmptyingDateError = if (newState.proposeEmptyingDate != _formState.value.proposeEmptyingDate) null
            else newState.proposeEmptyingDateError
        )
    }

    fun loadApplicationData(applicationId: String) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true)

            emptyingRepository.getSanitationCustomerDetails(applicationId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.data?.let { customerDetails ->
                            _formState.value = _formState.value.copy(
                                sanitationCustomerName = customerDetails.sanitationCustomerName ?: "",
                                sanitationCustomerContact = customerDetails.sanitationCustomerContact ?: "",
                                sanitationCustomerAddress = "", // Not available in current API response
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _formState.value = _formState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                    is Resource.Loading -> {
                        _formState.value = _formState.value.copy(isLoading = true)
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadFormOptions() {
        viewModelScope.launch {
            emptyingRepository.getEmptyingPurposes().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.data?.let { purposes ->
                            val purposeOptions = purposes.map { purpose ->
                                PurposeOptionData(
                                    id = purpose.id,
                                    type = purpose.type
                                )
                            }
                            _formState.value = _formState.value.copy(purposeOptions = purposeOptions)
                        }
                    }
                    is Resource.Error -> {
                        //
                    }
                    else -> {}
                }
            }

            emptyingRepository.getExperienceIssues().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.data?.let { issues ->
                            val issueOptions = issues.map { issue ->
                                PurposeOptionData(
                                    id = issue.id,
                                    type = issue.type
                                )
                            }
                            _formState.value = _formState.value.copy(experienceIssuesOptions = issueOptions)
                        }
                    }
                    is Resource.Error -> {
                        //
                    }
                    else -> {}
                }
            }
        }
    }

    private fun validateForm(state: EmptyingFormState): FormValidationResult {
        val errors = mutableMapOf<String, String>()

        if (state.applicantName.isBlank()) {
            errors["applicantName"] = "Applicant name is required"
        }

        if (state.applicantContact.isBlank()) {
            errors["applicantContact"] = "Applicant contact is required"
        } else if (state.applicantContact.length < 8) {
            errors["applicantContact"] = "Please enter a valid contact number"
        }

        if (state.proposeEmptyingDate.isBlank()) {
            errors["proposeEmptyingDate"] = "Proposed emptying date is required"
        } else {
            try {
                val proposedDate = LocalDate.parse(state.proposeEmptyingDate)
                if (proposedDate.isBefore(LocalDate.now())) {
                    errors["proposeEmptyingDate"] = "Proposed emptying date must be today or in the future"
                }
            } catch (e: Exception) {
                errors["proposeEmptyingDate"] = "Please enter a valid date"
            }
        }

        if (state.everEmptied == null) {
            errors["everEmptied"] = "Please specify if ever emptied before"
        }

        if (state.everEmptied == true && state.lastEmptiedDate.isBlank() && state.reasonForNoEmptiedDate.isBlank()) {
            errors["lastEmptiedDate"] = "Please provide last emptied date or reason for no date"
        }

        if (state.everEmptied == false && state.notEmptiedBeforeReason.isBlank()) {
            errors["notEmptiedBeforeReason"] = "Please provide reason for not being emptied before"
        }

        return FormValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    fun submitForm(applicationId: String?) {
        val currentState = _formState.value
        val validationResult = validateForm(currentState)

        if (!validationResult.isValid) {
            _formState.value = currentState.copy(
                applicantNameError = validationResult.errors["applicantName"],
                applicantContactError = validationResult.errors["applicantContact"],
                proposeEmptyingDateError = validationResult.errors["proposeEmptyingDate"],
                errorMessage = "Please fix the errors above"
            )
            return
        }

        viewModelScope.launch {
            _formState.value = currentState.copy(isLoading = true)
            _submitResult.value = Resource.Loading()

            val formRequest = EmptyingFormRequest(
                application_id = applicationId ?: "",
                service_type = "emptying",
                scheduled_date = currentState.proposeEmptyingDate,
                notes = buildString {
                    append("Applicant: ${currentState.applicantName} (${currentState.applicantContact})\n")
                    append("Purpose: ${currentState.purposeOfEmptyingRequest}\n")
                    if (currentState.otherEmptyingPurpose.isNotBlank()) {
                        append("Other Purpose: ${currentState.otherEmptyingPurpose}\n")
                    }
                    append("Ever Emptied: ${currentState.everEmptied}\n")
                    if (currentState.everEmptied == true) {
                        append("Last Emptied: ${currentState.lastEmptiedDate}\n")
                        if (currentState.reasonForNoEmptiedDate.isNotBlank()) {
                            append("Reason for no date: ${currentState.reasonForNoEmptiedDate}\n")
                        }
                    } else {
                        append("Not emptied reason: ${currentState.notEmptiedBeforeReason}\n")
                    }
                    append("Free service under PBC: ${currentState.freeServiceUnderPbc}\n")
                    if (currentState.sizeOfContainmentM3.isNotBlank()) {
                        append("Containment size: ${currentState.sizeOfContainmentM3} m³\n")
                    }
                    currentState.locationOfContainment?.let {
                        append("Location: ${it.displayName}\n")
                    }
                    currentState.presenceOfPumpingPoint?.let {
                        append("Pumping point: ${it.displayName}\n")
                    }
                }
            )

            val result = if (applicationId != null) {
                emptyingRepository.updateEmptyingForm(applicationId, formRequest)
            } else {
                emptyingRepository.submitEmptyingForm(formRequest)
            }

            result.collect { response ->
                _submitResult.value = response
                when (response) {
                    is Resource.Success -> {
                        _formState.value = currentState.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _formState.value = currentState.copy(
                            isLoading = false,
                            errorMessage = response.message
                        )
                    }
                    is Resource.Loading -> {
                        _formState.value = currentState.copy(isLoading = true)
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearForm() {
        _formState.value = EmptyingFormState()
        _submitResult.value = null
    }
}