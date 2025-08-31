package com.innovative.smis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "building_survey")
data class BuildingSurveyEntity(
    @PrimaryKey val bin: String,
    val sangkat: String = "",
    val village: String = "",
    val roadCode: String = "",
    val surveyDate: String = "",
    val enumeratorName: String = "",
    val respondentName: String = "",
    val respondentGender: String = "",
    val respondentContact: String = "",
    val respondentIsOwner: String = "",
    val ownerName: String = "",
    val ownerNameKhmer: String = "",
    val ownerGender: String = "",
    val ownerContact: String = "",
    val structureType: String = "",
    val floorCount: String = "",
    val householdServed: String = "",
    val buildingPhoto: String = "",
    val presenceOfToilet: String = "",
    val placeOfDefecation: String = "",
    val placeOfDefecationOther: String = "",
    val sharedToiletBin: String = "",
    val toiletConnection: String = "",
    val toiletConnectionOther: String = "",
    val sharedConnectionBin: String = "",
    val sewerBill: String = "",
    val sewerBillPhoto: String = "",
    val storageTankType: String = "",
    val storageTankTypeOther: String = "",
    val storageTankOutlet: String = "",
    val storageTankOutletOther: String = "",
    val storageTankSize: String = "",
    val storageTankYear: String = "",
    val storageTankAccessible: String = "",
    val storageTankEmptied: String = "",
    val storageTankLastEmptied: String = "",
    val waterConnection: String = "",
    val waterCustomerId: String = "",
    val waterMeterNumber: String = "",
    val waterMeterPhoto: String = "",
    val waterBillPhoto: String = "",
    val waterShared: String = "",
    val waterSharedBin: String = "",
    val syncStatus: String = "pending", // pending, synced, error
    val isOffline: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SurveyFormState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentSection: Int = 0,
    val totalSections: Int = 4,
    val validationErrors: Map<String, String> = emptyMap(),
    
    // A. Building Location Information
    val sangkat: String = "",
    val village: String = "",
    val roadCode: String = "",
    
    // B. Building Information  
    val respondentName: String = "",
    val respondentGender: String = "",
    val respondentContact: String = "",
    val respondentIsOwner: String = "",
    val ownerName: String = "",
    val ownerNameKhmer: String = "",
    val ownerGender: String = "",
    val ownerContact: String = "",
    val structureType: String = "",
    val floorCount: String = "",
    val householdServed: String = "",
    val buildingPhoto: String = "",
    
    // C. Toilet and Containment Information
    val presenceOfToilet: String = "",
    val placeOfDefecation: String = "",
    val placeOfDefecationOther: String = "",
    val sharedToiletBin: String = "",
    val toiletConnection: String = "",
    val toiletConnectionOther: String = "",
    val sharedConnectionBin: String = "",
    val sewerBill: String = "",
    val sewerBillPhoto: String = "",
    val storageTankType: String = "",
    val storageTankTypeOther: String = "",
    val storageTankOutlet: String = "",
    val storageTankOutletOther: String = "",
    val storageTankSize: String = "",
    val storageTankYear: String = "",
    val storageTankAccessible: String = "",
    val storageTankEmptied: String = "",
    val storageTankLastEmptied: String = "",
    
    // D. Water Source Information
    val waterConnection: String = "",
    val waterCustomerId: String = "",
    val waterMeterNumber: String = "",
    val waterMeterPhoto: String = "",
    val waterBillPhoto: String = "",
    val waterShared: String = "",
    val waterSharedBin: String = "",
    
    // Dropdown options
    val sangkatOptions: List<String> = emptyList(),
    val genderOptions: List<String> = listOf("male", "female", "other"),
    val yesNoOptions: List<String> = listOf("yes", "no"),
    val structureTypeOptions: List<String> = listOf("permanent", "semi-permanent", "temporary"),
    val placeOfDefecationOptions: List<String> = listOf("community_toilet", "shared_toilet", "open_defecation", "other"),
    val toiletConnectionOptions: List<String> = listOf("sewer", "shared_sewer", "storage_tank", "shared_storage_tank", "open_ground", "other"),
    val storageTankTypeOptions: List<String> = listOf("ring_close_bottom", "ring_open_bottom", "plastic_septic", "concrete_open_bottom", "concrete_close_bottom", "concrete_with_filter", "dont_know", "other"),
    val storageTankOutletOptions: List<String> = listOf("underground_infiltration", "discharge_ground", "discharge_channel", "connect_sewer", "connect_shared_sewer", "no_outlet", "dont_know", "other")
) {
    fun getProgress(): Float {
        val totalFields = 30 // Total form fields
        var filledFields = 0
        
        // Count filled mandatory fields
        if (sangkat.isNotEmpty()) filledFields++
        if (roadCode.isNotEmpty()) filledFields++
        if (respondentName.isNotEmpty()) filledFields++
        if (respondentGender.isNotEmpty()) filledFields++
        if (respondentContact.isNotEmpty()) filledFields++
        if (respondentIsOwner.isNotEmpty()) filledFields++
        if (structureType.isNotEmpty()) filledFields++
        if (floorCount.isNotEmpty()) filledFields++
        if (householdServed.isNotEmpty()) filledFields++
        if (buildingPhoto.isNotEmpty()) filledFields++
        if (presenceOfToilet.isNotEmpty()) filledFields++
        
        // Add conditional fields based on conditions
        if (respondentIsOwner == "no") {
            if (ownerName.isNotEmpty()) filledFields++
            if (ownerNameKhmer.isNotEmpty()) filledFields++
            if (ownerGender.isNotEmpty()) filledFields++
            if (ownerContact.isNotEmpty()) filledFields++
        }
        
        if (presenceOfToilet == "yes") {
            if (toiletConnection.isNotEmpty()) filledFields++
            if (toiletConnection in listOf("storage_tank", "shared_storage_tank")) {
                if (storageTankType.isNotEmpty()) filledFields++
                if (storageTankOutlet.isNotEmpty()) filledFields++
            }
        } else if (presenceOfToilet == "no") {
            if (placeOfDefecation.isNotEmpty()) filledFields++
        }
        
        if (waterConnection.isNotEmpty()) filledFields++
        
        return filledFields.toFloat() / totalFields
    }
    
    fun validateCurrentSection(): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        when (currentSection) {
            0 -> { // Building Location
                if (sangkat.isEmpty()) errors["sangkat"] = "Sangkat is required"
                if (roadCode.isEmpty()) errors["roadCode"] = "Road Code is required"
            }
            1 -> { // Building Information
                if (respondentName.isEmpty()) errors["respondentName"] = "Respondent name is required"
                if (respondentGender.isEmpty()) errors["respondentGender"] = "Gender is required"
                if (respondentContact.isEmpty()) errors["respondentContact"] = "Contact is required"
                if (!respondentContact.matches(Regex("^0\\d{8,10}$"))) {
                    errors["respondentContact"] = "Invalid contact format. Use 0XXXXXXXXX"
                }
                if (respondentIsOwner.isEmpty()) errors["respondentIsOwner"] = "Owner status is required"
                if (respondentIsOwner == "no") {
                    if (ownerName.isEmpty()) errors["ownerName"] = "Owner name is required"
                    if (ownerNameKhmer.isEmpty()) errors["ownerNameKhmer"] = "Owner name in Khmer is required"
                    if (ownerGender.isEmpty()) errors["ownerGender"] = "Owner gender is required"
                    if (ownerContact.isEmpty()) errors["ownerContact"] = "Owner contact is required"
                }
                if (structureType.isEmpty()) errors["structureType"] = "Structure type is required"
                if (floorCount.isEmpty()) errors["floorCount"] = "Floor count is required"
                if (householdServed.isEmpty()) errors["householdServed"] = "Household count is required"
                if (buildingPhoto.isEmpty()) errors["buildingPhoto"] = "Building photo is required"
            }
            2 -> { // Toilet Information
                if (presenceOfToilet.isEmpty()) errors["presenceOfToilet"] = "Toilet presence is required"
                if (presenceOfToilet == "yes") {
                    if (toiletConnection.isEmpty()) errors["toiletConnection"] = "Toilet connection is required"
                    if (toiletConnection == "other" && toiletConnectionOther.isEmpty()) {
                        errors["toiletConnectionOther"] = "Please specify other connection type"
                    }
                    if (toiletConnection in listOf("storage_tank", "shared_storage_tank")) {
                        if (storageTankType.isEmpty()) errors["storageTankType"] = "Storage tank type is required"
                        if (storageTankOutlet.isEmpty()) errors["storageTankOutlet"] = "Storage tank outlet is required"
                    }
                } else if (presenceOfToilet == "no") {
                    if (placeOfDefecation.isEmpty()) errors["placeOfDefecation"] = "Place of defecation is required"
                    if (placeOfDefecation == "other" && placeOfDefecationOther.isEmpty()) {
                        errors["placeOfDefecationOther"] = "Please specify other place"
                    }
                }
            }
            3 -> { // Water Information
                if (waterConnection.isEmpty()) errors["waterConnection"] = "Water connection status is required"
                if (waterConnection == "yes") {
                    if (waterCustomerId.isEmpty()) errors["waterCustomerId"] = "Water customer ID is required"
                    if (waterMeterNumber.isEmpty()) errors["waterMeterNumber"] = "Water meter number is required"
                }
            }
        }
        
        return errors
    }
    
    fun isCurrentSectionValid(): Boolean = validateCurrentSection().isEmpty()
    
    fun canProceedToNext(): Boolean = isCurrentSectionValid() && currentSection < totalSections - 1
    
    fun canSubmit(): Boolean = currentSection == totalSections - 1 && isCurrentSectionValid()
}

data class SurveyAlertState(
    val show: Boolean = false,
    val title: String = "",
    val message: String = ""
)