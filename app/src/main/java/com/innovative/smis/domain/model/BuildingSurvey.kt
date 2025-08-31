package com.innovative.smis.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuildingSurvey(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "bin")
    val bin: String,
    
    @Json(name = "sangkat")
    val sangkat: String,
    
    @Json(name = "village")
    val village: String?,
    
    @Json(name = "roadCode")
    val roadCode: String?,
    
    @Json(name = "respondentName")
    val respondentName: String,
    
    @Json(name = "respondentGender")
    val respondentGender: String,
    
    @Json(name = "respondentContact")
    val respondentContact: String?,
    
    @Json(name = "surveyedBy")
    val surveyedBy: String,
    
    @Json(name = "surveyDate")
    val surveyDate: String,
    
    // Extended survey fields
    @Json(name = "ownerName")
    val ownerName: String? = null,
    
    @Json(name = "ownerContact")
    val ownerContact: String? = null,
    
    @Json(name = "structureTypeId")
    val structureTypeId: String? = null,
    
    @Json(name = "functionalUseId")
    val functionalUseId: String? = null,
    
    @Json(name = "buildingUseId")
    val buildingUseId: String? = null,
    
    @Json(name = "numberOfFloors")
    val numberOfFloors: Int? = null,
    
    @Json(name = "householdServed")
    val householdServed: Int? = null,
    
    @Json(name = "populationServed")
    val populationServed: Int? = null,
    
    @Json(name = "isMainBuilding")
    val isMainBuilding: Boolean? = null,
    
    @Json(name = "floorArea")
    val floorArea: Double? = null,
    
    @Json(name = "constructionYear")
    val constructionYear: Int? = null,
    
    @Json(name = "waterSupply")
    val waterSupply: String? = null,
    
    @Json(name = "defecationPlaceId")
    val defecationPlaceId: String? = null,
    
    @Json(name = "numberOfToilets")
    val numberOfToilets: Int? = null,
    
    @Json(name = "toiletConnectionId")
    val toiletConnectionId: String? = null,
    
    @Json(name = "toiletCount")
    val toiletCount: Int? = null,
    
    @Json(name = "containmentPresentOnsite")
    val containmentPresentOnsite: Boolean? = null,
    
    @Json(name = "typeOfStorageTank")
    val typeOfStorageTank: String? = null,
    
    @Json(name = "storageTankConnection")
    val storageTankConnection: String? = null,
    
    @Json(name = "numberOfTanks")
    val numberOfTanks: Int? = null,
    
    @Json(name = "sizeOfTank")
    val sizeOfTank: Double? = null,
    
    // Additional comprehensive survey fields
    @Json(name = "distanceFromWell")
    val distanceFromWell: String? = null,
    
    @Json(name = "constructionDate")
    val constructionDate: String? = null,
    
    @Json(name = "lastEmptiedDate")
    val lastEmptiedDate: String? = null,
    
    @Json(name = "vacutugAccessible")
    val vacutugAccessible: Boolean? = null,
    
    @Json(name = "containmentLocation")
    val containmentLocation: String? = null,
    
    @Json(name = "accessToContainment")
    val accessToContainment: String? = null,
    
    @Json(name = "distanceHouseToContainment")
    val distanceHouseToContainment: String? = null,
    
    @Json(name = "waterCustomerId")
    val waterCustomerId: String? = null,
    
    @Json(name = "meterSerialNumber")
    val meterSerialNumber: String? = null,
    
    @Json(name = "sanitationSystem")
    val sanitationSystem: String? = null,
    
    @Json(name = "technology")
    val technology: String? = null,
    
    @Json(name = "compliance")
    val compliance: String? = null,
    
    @Json(name = "comments")
    val comments: String? = null,
    
    @Json(name = "createdAt")
    val createdAt: Long,
    
    @Json(name = "updatedAt")
    val updatedAt: Long
)