package com.innovative.smis.ui.features.buildingsurvey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.SurveyFormState
import com.innovative.smis.data.local.entity.BuildingSurveyEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ComprehensiveSurveyViewModel(
    private val repository: com.innovative.smis.data.repository.BuildingSurveyRepository
) : ViewModel() {
    
    private val _formState = MutableStateFlow(SurveyFormState())
    val formState = _formState.asStateFlow()
    
    init {
        loadDropdownOptions()
    }
    
    private fun loadDropdownOptions() {
        // Load sangkat options (in real app, this would come from API)
        val sangkatOptions = listOf(
            "Boeng Keng Kang I",
            "Boeng Keng Kang II", 
            "Boeng Keng Kang III",
            "Chamkar Mon",
            "Tonle Bassac",
            "Wat Phnom",
            "Chey Chumnas",
            "Phsar Thmei I",
            "Phsar Thmei II",
            "Phsar Thmei III"
        )
        
        _formState.update { 
            it.copy(sangkatOptions = sangkatOptions)
        }
    }
    
    fun loadSurvey(bin: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            
            try {
                // In real app, load existing survey data from repository
                // For now, just simulate loading
                kotlinx.coroutines.delay(500)
                
                _formState.update { 
                    it.copy(
                        isLoading = false,
                        roadCode = bin // Pre-fill with BIN
                    )
                }
            } catch (e: Exception) {
                _formState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load survey: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun updateFormState(newState: SurveyFormState) {
        _formState.update { newState }
        validateCurrentSection()
    }
    
    private fun validateCurrentSection() {
        val currentState = _formState.value
        val validationErrors = currentState.validateCurrentSection()
        
        _formState.update { 
            it.copy(validationErrors = validationErrors)
        }
    }
    
    fun nextSection() {
        val currentState = _formState.value
        if (currentState.canProceedToNext()) {
            _formState.update { 
                it.copy(
                    currentSection = it.currentSection + 1,
                    validationErrors = emptyMap()
                )
            }
        }
    }
    
    fun previousSection() {
        val currentState = _formState.value
        if (currentState.currentSection > 0) {
            _formState.update { 
                it.copy(
                    currentSection = it.currentSection - 1,
                    validationErrors = emptyMap()
                )
            }
        }
    }
    
    fun submitSurvey(bin: String?) {
        viewModelScope.launch {
            val currentState = _formState.value
            
            if (!currentState.canSubmit()) {
                validateCurrentSection()
                return@launch
            }
            
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            
            try {
                val surveyEntity = BuildingSurveyEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    bin = bin ?: currentState.roadCode,
                    sangkat = currentState.sangkat,
                    village = currentState.village,
                    roadCode = currentState.roadCode,
                    isMainBuilding = currentState.isMainBuilding,
                    buildingNo = currentState.buildingNo,
                    taxCode = currentState.taxCode,
                    streetName = currentState.streetName,
                    surveyDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    containmentLocation = currentState.containmentLocation,
                    containmentLength = currentState.containmentLength,
                    containmentWidth = currentState.containmentWidth,
                    containmentDepth = currentState.containmentDepth,
                    containmentVolume = currentState.containmentVolume,
                    pitDiameter = currentState.pitDiameter,
                    pitDepth = currentState.pitDepth,
                    drainCode = currentState.drainCode,
                    sewerCode = currentState.sewerCode,
                    respondentName = currentState.respondentName,
                    respondentGender = currentState.respondentGender,
                    respondentContact = currentState.respondentContact,
                    respondentIsOwner = currentState.respondentIsOwner,
                    ownerName = currentState.ownerName,
                    ownerNameKhmer = currentState.ownerNameKhmer,
                    ownerGender = currentState.ownerGender,
                    ownerContact = currentState.ownerContact,
                    structureType = currentState.structureType,
                    floorCount = currentState.floorCount,
                    householdServed = currentState.householdServed,
                    populationServed = currentState.populationServed,
                    buildingPhoto = currentState.buildingPhoto,
                    functionalUse = currentState.functionalUse,
                    buildingUse = currentState.buildingUse,
                    officeName = currentState.officeName,
                    constructionDate = currentState.constructionDate,
                    presenceOfToilet = currentState.presenceOfToilet,
                    placeOfDefecation = currentState.placeOfDefecation,
                    placeOfDefecationOther = currentState.placeOfDefecationOther,
                    sharedToiletBin = currentState.sharedToiletBin,
                    toiletConnection = currentState.toiletConnection,
                    toiletConnectionOther = currentState.toiletConnectionOther,
                    sharedConnectionBin = currentState.sharedConnectionBin,
                    sewerBill = currentState.sewerBill,
                    sewerBillPhoto = currentState.sewerBillPhoto,
                    storageTankType = currentState.storageTankType,
                    storageTankTypeOther = currentState.storageTankTypeOther,
                    storageTankOutlet = currentState.storageTankOutlet,
                    storageTankOutletOther = currentState.storageTankOutletOther,
                    storageTankSize = currentState.storageTankSize,
                    storageTankYear = currentState.storageTankYear,
                    storageTankAccessible = currentState.storageTankAccessible,
                    storageTankEmptied = currentState.storageTankEmptied,
                    storageTankLastEmptied = currentState.storageTankLastEmptied,
                    waterConnection = currentState.waterConnection,
                    waterCustomerId = currentState.waterCustomerId,
                    waterMeterNumber = currentState.waterMeterNumber,
                    waterMeterPhoto = currentState.waterMeterPhoto,
                    waterBillPhoto = currentState.waterBillPhoto,
                    waterShared = currentState.waterShared,
                    waterSharedBin = currentState.waterSharedBin,
                    syncStatus = "pending",
                    isOffline = true
                )
                
                val result = repository.saveSurveyOffline(surveyEntity)
                
                if (result is com.innovative.smis.util.common.Resource.Success) {
                    _formState.update { 
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Survey submitted successfully and saved offline. It will sync when internet is available."
                        )
                    }
                } else {
                     _formState.update { 
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.message ?: "Failed to save survey offline"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _formState.update { 
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to submit survey: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _formState.update { it.copy(errorMessage = null) }
    }
    
    fun clearSuccess() {
        _formState.update { it.copy(successMessage = null) }
    }
}