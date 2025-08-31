package com.innovative.smis.ui.features.buildingsurvey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.BuildingSurveyFormState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BuildingSurveyViewModel : ViewModel() {
    
    private val _formState = MutableStateFlow(BuildingSurveyFormState())
    val formState: StateFlow<BuildingSurveyFormState> = _formState.asStateFlow()

    fun updateFormState(newState: BuildingSurveyFormState) {
        _formState.value = newState
    }

    fun loadBuildingSurvey(bin: String) {
        viewModelScope.launch {
            try {
                _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
                
                // TODO: Load building survey data from repository
                // val surveyData = repository.getBuildingSurvey(bin)
                // _formState.value = _formState.value.copy(
                //     isLoading = false,
                //     bin = surveyData.bin,
                //     sangkat = surveyData.sangkat,
                //     // ... other fields
                // )
                
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    bin = bin
                )
                
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load building survey: ${e.message}"
                )
            }
        }
    }

    fun loadDropdownOptions() {
        viewModelScope.launch {
            try {
                // TODO: Load dropdown options from repository
                // val sangkats = repository.getSangkats()
                // val roadCodes = repository.getRoadCodes()
                // val structureTypes = repository.getStructureTypes()
                // val functionalUses = repository.getFunctionalUses()
                // val buildingUses = repository.getBuildingUses()
                
                _formState.value = _formState.value.copy(
                    sangkats = emptyList(),
                    roadCodes = emptyList(),
                    structureTypes = emptyList(),
                    functionalUses = emptyList(),
                    buildingUses = emptyList()
                )
                
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    errorMessage = "Failed to load dropdown options: ${e.message}"
                )
            }
        }
    }

    fun submitSurvey(bin: String?) {
        viewModelScope.launch {
            try {
                _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
                
                val currentState = _formState.value
                
                val validationErrors = validateForm(currentState)
                if (validationErrors.isNotEmpty()) {
                    _formState.value = currentState.copy(
                        isLoading = false,
                        binError = validationErrors["bin"],
                        respondentNameError = validationErrors["respondentName"]
                    )
                    return@launch
                }
                
                // TODO: Submit to repository
                // repository.submitBuildingSurvey(currentState)
                
                _formState.value = _formState.value.copy(
                    isLoading = false
                    //
                )
                
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to submit survey: ${e.message}"
                )
            }
        }
    }

    private fun validateForm(state: BuildingSurveyFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        if (state.bin.isBlank()) {
            errors["bin"] = "BIN is required"
        }
        
        if (state.respondentName.isBlank()) {
            errors["respondentName"] = "Respondent name is required"
        }

        return errors
    }
}