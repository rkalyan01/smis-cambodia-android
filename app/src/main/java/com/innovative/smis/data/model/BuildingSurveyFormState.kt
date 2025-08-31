package com.innovative.smis.data.model

data class BuildingSurveyFormState(
    // Basic Information
    val bin: String = "",
    val binError: String? = null,
    val sangkat: String = "",
    val village: String = "",
    val roadCode: String = "",
    
    // Respondent Information
    val respondentName: String = "",
    val respondentNameError: String? = null,
    val respondentGender: RespondentGender? = null,
    val respondentContact: String = "",
    
    // Owner Information
    val ownerName: String = "",
    val ownerContact: String = "",
    
    // Building Information
    val structureType: String = "",
    val functionalUse: String = "",
    val buildingUse: String = "",
    
    // Additional Building Details
    val numberOfFloors: String = "",
    val householdServed: String = "",
    val populationServed: String = "",
    val floorArea: String = "",
    val isMainBuilding: Boolean = false,
    val constructionYear: String = "",
    
    // Water and Sanitation
    val waterSupply: WaterSupply? = null,
    val defecationPlace: String = "",
    val numberOfToilets: String = "",
    val toiletCount: String = "",
    val toiletConnection: String = "",
    val containmentPresentOnsite: Boolean = false,
    val typeOfStorageTank: String = "",
    val storageTankConnection: String = "",
    val numberOfTanks: String = "",
    val sizeOfTank: String = "",
    val distanceFromWell: String = "",
    val constructionDate: String = "",
    val lastEmptiedDate: String = "",
    val vacutugAccessible: VacutugAccessible? = null,
    val containmentLocation: String = "",
    val distanceHouseToContainment: String = "",
    val waterCustomerId: String = "",
    val meterSerialNumber: String = "",
    val sanitationSystem: SanitationSystem? = null,
    val technology: Technology? = null,
    val compliance: Boolean = false,
    val comments: String = "",
    
    // UI State
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    
    // Data Lists
    val sangkats: List<SangkatData> = emptyList(),
    val roadCodes: List<RoadCodeData> = emptyList(),
    val structureTypes: List<StructureTypeData> = emptyList(),
    val functionalUses: List<FunctionalUseData> = emptyList(),
    val buildingUses: List<BuildingUseData> = emptyList(),
    val defecationPlaces: List<DefecationPlaceData> = emptyList(),
    val toiletConnections: List<ToiletConnectionData> = emptyList(),
    val storageTankTypes: List<StorageTankTypeData> = emptyList(),
    val storageTankConnections: List<StorageTankConnectionData> = emptyList()
)

enum class RespondentGender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other")
}

data class SangkatData(
    val id: String,
    val sangkatName: String
)

data class RoadCodeData(
    val id: String,
    val roadCode: String
)

data class StructureTypeData(
    val id: String,
    val type: String
)

data class FunctionalUseData(
    val id: String,
    val type: String
)

data class BuildingUseData(
    val id: String,
    val type: String
)

enum class WaterSupply(val displayName: String) {
    PIPED("Piped Water"),
    WELL("Well Water"),
    SURFACE("Surface Water"),
    RAINWATER("Rainwater"),
    OTHER("Other")
}

data class DefecationPlaceData(
    val id: String,
    val type: String
)

data class ToiletConnectionData(
    val id: String,
    val type: String
)

data class StorageTankTypeData(
    val id: String,
    val type: String
)

data class StorageTankConnectionData(
    val id: String,
    val type: String
)

enum class VacutugAccessible(val displayName: String) {
    EASILY_ACCESSIBLE("Easily Accessible"),
    ACCESSIBLE_WITH_DIFFICULTY("Accessible with Difficulty"),
    NOT_ACCESSIBLE("Not Accessible")
}

enum class SanitationSystem(val displayName: String) {
    SEPTIC_TANK("Septic Tank"),
    PIT_LATRINE("Pit Latrine"),
    SEWER_CONNECTION("Sewer Connection"),
    COMPOSTING_TOILET("Composting Toilet"),
    OTHER("Other")
}

enum class Technology(val displayName: String) {
    CONVENTIONAL("Conventional"),
    IMPROVED("Improved"),
    ECOLOGICAL("Ecological"),
    HYBRID("Hybrid")
}