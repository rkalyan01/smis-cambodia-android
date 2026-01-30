package com.innovative.smis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// BuildingSurveyEntity moved to data/local/entity/BuildingSurveyEntity.kt

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
    val isMainBuilding: String = "yes", // React Native uses boolean but converted to yes/no string often
    val buildingNo: String = "",
    val taxCode: String = "",
    val streetName: String = "",
    
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
    val populationServed: String = "",
    val buildingPhoto: String = "",
    val functionalUse: String = "",
    val buildingUse: String = "",
    val officeName: String = "",
    val constructionDate: String = "",
    val surveyDate: String = "",
    
    // C. Toilet and Containment Information
    val presenceOfToilet: String = "",
    val placeOfDefecation: String = "",
    val placeOfDefecationOther: String = "",
    val sharedToiletBin: String = "",
    val toiletConnection: String = "", // This is "technology" in RN
    val toiletConnectionOther: String = "",
    val sharedConnectionBin: String = "",
    val drainCode: String = "",
    val sewerCode: String = "",
    val sewerBill: String = "",
    val sewerBillPhoto: String = "",
    
    // Containment Details
    val containmentLocation: String = "",
    val containmentLength: String = "",
    val containmentWidth: String = "",
    val containmentDepth: String = "",
    val containmentVolume: String = "",
    val pitDiameter: String = "",
    val pitDepth: String = "",
    
    val storageTankType: String = "",
    val storageTankTypeOther: String = "",
    val storageTankOutlet: String = "",
    val storageTankOutletOther: String = "",
    val storageTankSize: String = "",
    val storageTankYear: String = "", // Construction date of tank
    val compliance: String = "", // Septic tank compliance
    val storageTankAccessible: String = "", // Vacutug accessible
    val storageTankEmptied: String = "",
    val storageTankLastEmptied: String = "",
    
    // D. Water Source Information
    
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
    val dontKnowOptions: List<String> = listOf("yes", "no", "dont_know"),
    val structureTypeOptions: List<String> = listOf("permanent", "semi-permanent", "temporary"),
    val functionalUseOptions: List<String> = listOf("health", "business", "residental"),
    val buildingUseOptions: List<String> = listOf("Option 1", "Option 2", "Option 3", "Option 4"), // Placeholder from RN
    val placeOfDefecationOptions: List<String> = listOf("community_toilet", "shared_toilet", "open_defecation", "other"),
    val toiletConnectionOptions: List<String> = listOf(
        "0", // Select sanitation system technology
        "1", // Anaerobic Digestor
        "2", // Cesspool/Holding tank
        "3", // Communal Septic Tank (from PT CT)
        "4", // Dehydration Toilet System
        "5", // DEWATS Online
        "6", // Directly to natural water body
        "7", // Directly to sewage network
        "8", // Directly to stormwater draim
        "9", // Directly to surrounding environment
        "10", // Double pit with soak away pit
        "11", // Septic tank connected to sewerage network
        "12", // Septic tank without soak away pit
        "13", // Septic tank with soak away pit
        "14", // Shared Septic tank
        "15"  // Single pit
    ),
    val containmentLocationOptions: List<String> = listOf("outside", "inside", "outside_2"),
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
                if (isMainBuilding == "no" && buildingNo.isEmpty()) errors["buildingNo"] = "Building No is required"
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
                // functionalUse logic
                if (functionalUse.isNotEmpty() && functionalUse != "residental" && officeName.isEmpty()) {
                    errors["officeName"] = "Office/Business name is required"
                }
            }
            2 -> { // Toilet Information
                if (presenceOfToilet.isEmpty()) errors["presenceOfToilet"] = "Toilet presence is required"
                
                if (presenceOfToilet == "yes") {
                     // Toilet Connection is the Technology ID now (0-15)
                    if (toiletConnection.isEmpty() || toiletConnection == "0") errors["toiletConnection"] = "Technology selection is required"
                    
                    // Specific Tech Validations
                    if (toiletConnection == "13" && sharedConnectionBin.isEmpty()) {
                         errors["sharedConnectionBin"] = "Pre-connected BIN is required"
                    }
                    if (toiletConnection == "8" && drainCode.isEmpty()) {
                        errors["drainCode"] = "Drain Code is required"
                    }
                    if (toiletConnection == "11" && sewerCode.isEmpty()) {
                        errors["sewerCode"] = "Sewer Code is required"
                    }
                    
                    // Containment Logic (Tech 2, 11, 12 -> Tank Details)
                    if (toiletConnection in listOf("2", "11", "12")) {
                        if (containmentLength.isEmpty()) errors["containmentLength"] = "Length is required"
                        if (containmentWidth.isEmpty()) errors["containmentWidth"] = "Width is required"
                        if (containmentDepth.isEmpty()) errors["containmentDepth"] = "Depth is required"
                         // Construction date is handled by separate field or simplified
                    }
                    
                    // Pit Logic (Tech 10, 15 -> Pit Details)
                    if (toiletConnection in listOf("10", "15")) {
                         if (pitDiameter.isEmpty()) errors["pitDiameter"] = "Pit Diameter is required"
                         if (pitDepth.isEmpty()) errors["pitDepth"] = "Pit Depth is required"
                         if (containmentLocation.isEmpty()) errors["containmentLocation"] = "Containment Location is required"
                    }
                    
                } else if (presenceOfToilet == "no") {
                    if (placeOfDefecation.isEmpty()) errors["placeOfDefecation"] = "Place of defecation is required"
                    if (placeOfDefecation == "other" && placeOfDefecationOther.isEmpty()) {
                        errors["placeOfDefecationOther"] = "Please specify other place"
                    }
                    if (placeOfDefecation == "shared_toilet" && sharedToiletBin.isEmpty()) {
                        errors["sharedToiletBin"] = "Shared Toilet BIN is required"
                    }
                }
            }
            3 -> { // Water Information
                if (waterConnection.isEmpty()) errors["waterConnection"] = "Water connection status is required"
                if (waterConnection == "yes") {
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