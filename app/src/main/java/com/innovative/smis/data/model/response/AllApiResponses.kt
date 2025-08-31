package com.innovative.smis.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// =====================================
// AUTHENTICATION RESPONSES  
// =====================================

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "status") val status: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "token") val token: String?,
    @Json(name = "data") val data: UserData?
)

@JsonClass(generateAdapter = true)
data class UserData(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "id") val id: Int,
    @Json(name = "eto_id") val etoId: Int,
    @Json(name = "permissions") val permissions: List<UserPermission>?
)

@JsonClass(generateAdapter = true)
data class UserPermission(
    @Json(name = "View Map") val viewMap: Boolean? = false,
    @Json(name = "Edit Building Survey") val editBuildingSurvey: Boolean? = false,
    @Json(name = "Emtying Scheduling") val emptyingScheduling: Boolean? = false,
    @Json(name = "Site Preparation") val sitePreparation: Boolean? = false,
    @Json(name = "Emptying") val emptying: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class LogoutResponse(
    @Json(name = "message") val message: String
)

// =====================================
// TASK MANAGEMENT RESPONSES
// =====================================

@JsonClass(generateAdapter = true)
data class TaskResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: List<TaskData>?
)

@JsonClass(generateAdapter = true)
data class TaskData(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "status") val status: String,
    @Json(name = "priority") val priority: String,
    @Json(name = "assigned_to") val assignedTo: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "due_date") val dueDate: String?
)

// =====================================
// EMPTYING SERVICE RESPONSES
// =====================================

@JsonClass(generateAdapter = true)
data class ApplicationListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: List<Application>?
)

@JsonClass(generateAdapter = true)
data class Application(
    @Json(name = "id") val id: String,
    @Json(name = "reference_number") val reference_number: String?,
    @Json(name = "application_status") val status: String,
    @Json(name = "applicant_name") val applicant_name: String?,
    @Json(name = "address") val address: String?
)

@JsonClass(generateAdapter = true)
data class EmptyingFormResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: EmptyingFormData?
)

@JsonClass(generateAdapter = true)
data class EmptyingFormData(
    @Json(name = "id") val id: String,
    @Json(name = "application_id") val applicationId: String,
    @Json(name = "form_data") val formData: String?,
    @Json(name = "submitted_at") val submittedAt: String?
)

@JsonClass(generateAdapter = true)
data class EmptyingFormRequest(
    @Json(name = "application_id") val application_id: String,
    @Json(name = "service_type") val service_type: String,
    @Json(name = "scheduled_date") val scheduled_date: String?,
    @Json(name = "notes") val notes: String?
)

@JsonClass(generateAdapter = true)
data class EmptyingDashboardDataResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: EmptyingDashboardData?
)

@JsonClass(generateAdapter = true)
data class EmptyingDashboardData(
    @Json(name = "total_applications") val total_applications: Int,
    @Json(name = "pending_services") val pending_services: Int,
    @Json(name = "completed_services") val completed_services: Int
)

@JsonClass(generateAdapter = true)
data class EtoNamesResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<EtoName>?
)

@JsonClass(generateAdapter = true)
data class EtoName(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TruckNumbersResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<TruckNumber>?
)

@JsonClass(generateAdapter = true)
data class TruckNumber(
    @Json(name = "id") val id: String,
    @Json(name = "truck_number") val truck_number: String,
    @Json(name = "capacity") val capacity: String?
)

// =====================================
// BUILDING SURVEY RESPONSES
// =====================================

@JsonClass(generateAdapter = true)
data class ContainmentIssueResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: Map<String, String>?
)



@JsonClass(generateAdapter = true)
data class SanitationCustomerResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: SanitationCustomerData?
)

@JsonClass(generateAdapter = true)
data class SanitationCustomerData(
    @Json(name = "sanitation_customer_name") val sanitationCustomerName: String?,
    @Json(name = "sanitation_customer_contact") val sanitationCustomerContact: String?,
    @Json(name = "pbc_customer_type") val pbcCustomerType: String?,
    @Json(name = "ever_emptied") val everEmptied: Boolean?,
    @Json(name = "last_emptied_year") val lastEmptiedYear: Int?,
    @Json(name = "emptied_nodate_reason") val emptiedNodateReason: String?,
    @Json(name = "not_emptied_before_reason") val notEmptiedBeforeReason: String?,
    @Json(name = "size_of_storage_tank_m3") val sizeOfStorageTankM3: String?,
    @Json(name = "construction_year") val constructionYear: Int?,
    @Json(name = "accessibility") val accessibility: Boolean?,
    @Json(name = "free_service_under_pbc") val freeServiceUnderPbc: Boolean?
)

// =====================================
// BUILDING SURVEY RESPONSES  
// =====================================

@JsonClass(generateAdapter = true)
data class SangkatResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: List<Sangkat>?
)

@JsonClass(generateAdapter = true)
data class Sangkat(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class WmsBuildingResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: WmsBuildingData?
)

@JsonClass(generateAdapter = true)
data class WmsBuildingData(
    @Json(name = "url") val url: String,
    @Json(name = "layers") val layers: List<String>?,
    @Json(name = "building_surveys") val building_surveys: String?
)

@JsonClass(generateAdapter = true)
data class WmsRoadResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: WmsRoadData?
)

@JsonClass(generateAdapter = true)
data class WmsRoadData(
    @Json(name = "road_networks") val road_networks: String
)

@JsonClass(generateAdapter = true)
data class WmsSewerResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: WmsSewerData?
)

@JsonClass(generateAdapter = true)
data class WmsSewerData(
    @Json(name = "sewer_networks") val sewer_networks: String
)

@JsonClass(generateAdapter = true)
data class WmsSangkatResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: WmsSangkatData?
)

@JsonClass(generateAdapter = true)
data class WmsSangkatData(
    @Json(name = "communes_sangkats") val communes_sangkats: String
)

@JsonClass(generateAdapter = true)
data class WmsGeometry(
    @Json(name = "type") val type: String,
    @Json(name = "coordinates") val coordinates: List<List<List<Double>>>?
)

@JsonClass(generateAdapter = true)
data class WfsBuildingLayerResponse(
    @Json(name = "type") val type: String,
    @Json(name = "features") val features: List<WfsBuildingFeature>?
)

@JsonClass(generateAdapter = true)
data class WfsBuildingFeature(
    @Json(name = "type") val type: String,
    @Json(name = "id") val id: String?,
    @Json(name = "properties") val properties: WfsBuildingProperties?,
    @Json(name = "geometry") val geometry: WfsGeometry?
)

@JsonClass(generateAdapter = true)
data class WfsBuildingProperties(
    @Json(name = "bin") val bin: String,
    @Json(name = "is_surveyed") val is_surveyed: Boolean,
    @Json(name = "is_auxiliary") val is_auxiliary: Boolean
)

@JsonClass(generateAdapter = true)
data class WfsGeometry(
    @Json(name = "type") val type: String,
    @Json(name = "coordinates") val coordinates: List<List<List<Double>>>?
)

@JsonClass(generateAdapter = true)
data class WfsBuildingSurveyLayerResponse(
    @Json(name = "type") val type: String,
    @Json(name = "features") val features: List<WfsSurveyFeature>?
)

@JsonClass(generateAdapter = true)
data class WfsSurveyFeature(
    @Json(name = "type") val type: String,
    @Json(name = "properties") val properties: WfsSurveyProperties?,
    @Json(name = "geometry") val geometry: WfsGeometry?
)

@JsonClass(generateAdapter = true)
data class WfsSurveyProperties(
    @Json(name = "survey_id") val survey_id: String,
    @Json(name = "building_bin") val building_bin: String,
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class BuildingSurveyResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: BuildingSurveyData?
)

@JsonClass(generateAdapter = true)
data class BuildingSurveyData(
    @Json(name = "id") val id: String,
    @Json(name = "bin") val bin: String,
    @Json(name = "survey_data") val surveyData: String?,
    @Json(name = "surveyed_by") val surveyedBy: String?,
    @Json(name = "survey_date") val surveyDate: String?
)

@JsonClass(generateAdapter = true)
data class BuildingSurveyRequest(
    @Json(name = "bin") val bin: String,
    @Json(name = "sangkat") val sangkat: String,
    @Json(name = "village") val village: String?,
    @Json(name = "road_code") val roadCode: String?,
    @Json(name = "respondent_name") val respondentName: String,
    @Json(name = "respondent_gender") val respondentGender: String,
    @Json(name = "respondent_contact") val respondentContact: String?,
    @Json(name = "owner_name") val ownerName: String?,
    @Json(name = "owner_contact") val ownerContact: String?,
    @Json(name = "structure_type_id") val structureTypeId: String?,
    @Json(name = "functional_use_id") val functionalUseId: String?,
    @Json(name = "building_use_id") val buildingUseId: String?,
    @Json(name = "number_of_floors") val numberOfFloors: Int?,
    @Json(name = "household_served") val householdServed: Int?,
    @Json(name = "population_served") val populationServed: Int?,
    @Json(name = "is_main_building") val isMainBuilding: Boolean?,
    @Json(name = "floor_area") val floorArea: Double?,
    @Json(name = "construction_year") val constructionYear: Int?,
    @Json(name = "water_supply") val waterSupply: String?,
    @Json(name = "defecation_place_id") val defecationPlaceId: String?,
    @Json(name = "number_of_toilets") val numberOfToilets: Int?,
    @Json(name = "toilet_connection_id") val toiletConnectionId: String?,
    @Json(name = "toilet_count") val toiletCount: Int?,
    @Json(name = "containment_present_onsite") val containmentPresentOnsite: Boolean?,
    @Json(name = "type_of_storage_tank") val typeOfStorageTank: String?,
    @Json(name = "storage_tank_connection") val storageTankConnection: String?,
    @Json(name = "number_of_tanks") val numberOfTanks: Int?,
    @Json(name = "size_of_tank") val sizeOfTank: Double?,
    @Json(name = "distance_from_well") val distanceFromWell: String?,
    @Json(name = "construction_date") val constructionDate: String?,
    @Json(name = "last_emptied_date") val lastEmptiedDate: String?,
    @Json(name = "vacutug_accessible") val vacutugAccessible: Boolean?,
    @Json(name = "containment_location") val containmentLocation: String?,
    @Json(name = "access_to_containment") val accessToContainment: String?,
    @Json(name = "distance_house_to_containment") val distanceHouseToContainment: String?,
    @Json(name = "water_customer_id") val waterCustomerId: String?,
    @Json(name = "meter_serial_number") val meterSerialNumber: String?,
    @Json(name = "sanitation_system") val sanitationSystem: String?,
    @Json(name = "technology") val technology: String?,
    @Json(name = "compliance") val compliance: String?,
    @Json(name = "comments") val comments: String?,
    @Json(name = "surveyed_by") val surveyedBy: String,
    @Json(name = "survey_date") val surveyDate: String
)

@JsonClass(generateAdapter = true)
data class SurveyDropdownResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<SurveyDropdownItem>?
)

// =====================================
// EMPTYING SCHEDULING RESPONSES
// =====================================

// Removed EmptyingReasonsResponse and ContainmentIssuesResponse classes
// These are now defined in SimpleApiResponses.kt without KSP annotations
// to avoid compilation issues

// EmptyingSchedulingFormRequest moved to data/model/request/EmptyingSchedulingFormRequest.kt
// where it properly belongs as a request class, not a response class

// =====================================
// SITE PREPARATION RESPONSES
// =====================================

@JsonClass(generateAdapter = true)
data class SitePreparationCustomerDetailsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: SitePreparationCustomerDetails?
)

@JsonClass(generateAdapter = true)
data class SitePreparationCustomerDetails(
    @Json(name = "sanitation_customer_name") val sanitationCustomerName: String?,
    @Json(name = "sanitation_customer_contact") val sanitationCustomerContact: String?,
    @Json(name = "sanitation_customer_address") val sanitationCustomerAddress: String?,
    @Json(name = "pbc_customer_type") val pbcCustomerType: String?,
    @Json(name = "free_service_under_pbc") val freeServiceUnderPbc: Boolean?,
    @Json(name = "last_emptied_year") val lastEmptiedYear: String?,
    @Json(name = "ever_emptied") val everEmptied: Boolean?,
    @Json(name = "emptied_nodate_reason") val emptiedNodateReason: String?,
    @Json(name = "not_emptied_before_reason") val notEmptiedBeforeReason: String?,
    @Json(name = "purpose_of_emptying") val purposeOfEmptying: String?,
    @Json(name = "other_emptying_purpose") val otherEmptyingPurpose: String?,
    @Json(name = "additional_repairing") val additionalRepairing: String?,
    @Json(name = "other_additional_repairing") val otherAdditionalRepairing: String?,
    @Json(name = "extra_payment_required") val extraPaymentRequired: Boolean?,
    @Json(name = "amount_of_extra_payment") val amountOfExtraPayment: String?,
    @Json(name = "proposed_emptying_date") val proposedEmptyingDate: String?,
    @Json(name = "applicant_name") val applicantName: String?,
    @Json(name = "applicant_contact") val applicantContact: String?
)

@JsonClass(generateAdapter = true)
data class SitePreparationFormRequest(
    @Json(name = "applicant_name") val applicantName: String?,
    @Json(name = "applicant_contact") val applicantContact: String?,
    @Json(name = "customer_name") val customerName: String?,
    @Json(name = "customer_contact") val customerContact: String?,
    @Json(name = "purpose_of_emptying") val purposeOfEmptying: String?,
    @Json(name = "other_emptying_purpose") val otherEmptyingPurpose: String?,
    @Json(name = "ever_emptied") val everEmptied: Boolean?,
    @Json(name = "last_emptied_date") val lastEmptiedDate: String?,
    @Json(name = "not_emptied_before_reason") val notEmptiedBeforeReason: String?,
    @Json(name = "reason_for_no_emptied_date") val reasonForNoEmptiedDate: String?,
    @Json(name = "additional_repairing") val additionalRepairing: String?,
    @Json(name = "other_additional_repairing") val otherAdditionalRepairing: String?,
    @Json(name = "extra_payment_required") val extraPaymentRequired: Boolean?,
    @Json(name = "amount_of_extra_payment") val amountOfExtraPayment: String?,
    @Json(name = "need_reschedule") val needReschedule: Boolean?,
    @Json(name = "new_proposed_emptying_date") val newProposedEmptyingDate: String?
)



@JsonClass(generateAdapter = true)
data class StorageTypeResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class StorageConnectionResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: Map<String, String>?
)


@JsonClass(generateAdapter = true)
data class DesludgingVehicleResponse(
    @Json(name = "eto_id") val etoId: String,
    @Json(name = "vehicles") val vehicles: List<DesludgingVehicle>
)

@JsonClass(generateAdapter = true)
data class DesludgingVehicle(
    @Json(name = "id") val id: Int,
    @Json(name = "license_plate_no") val licensePlateNo: String,
    @Json(name = "status") val status: String? = null
)

// API endpoints mock for ContainmentForm tests
@JsonClass(generateAdapter = true)
data class ContainmentMockRequest(
    @Json(name = "sanitation_customer_id") val sanitationCustomerId: String,
    @Json(name = "type_of_storage_tank") val typeOfStorageTank: String,
    @Json(name = "other_type_of_storage_tank") val otherTypeOfStorageTank: String,
    @Json(name = "storage_tank_connection") val storageTankConnection: String,
    @Json(name = "other_storage_tank_connection") val otherStorageTankConnection: String,
    @Json(name = "size_of_storage_tank_m3") val sizeOfStorageTankM3: String,
    @Json(name = "construction_year") val constructionYear: String,
    @Json(name = "accessibility") val accessibility: String,
    @Json(name = "ever_emptied") val everEmptied: String,
    @Json(name = "last_emptied_year") val lastEmptiedYear: String
)

@JsonClass(generateAdapter = true)
data class SurveyDropdownItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String?
)

@JsonClass(generateAdapter = true)
data class RoadCodeResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<RoadCode>?
)

@JsonClass(generateAdapter = true)
data class RoadCode(
    @Json(name = "id") val id: String,
    @Json(name = "road_code") val roadCode: String,
    @Json(name = "road_name") val roadName: String?
)

@JsonClass(generateAdapter = true)
data class EmptyingPurposeResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<EmptyingPurpose>?
)

@JsonClass(generateAdapter = true)
data class EmptyingPurpose(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String
)

@JsonClass(generateAdapter = true)
data class ExperienceIssuesResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<ExperienceIssue>?
)

@JsonClass(generateAdapter = true)
data class ExperienceIssue(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String
)

// =====================================
// COMMON ERROR RESPONSES
// =====================================

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String?,
    @Json(name = "errors") val errors: Map<String, List<String>>? = null
)

@JsonClass(generateAdapter = true)
data class GenericApiResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: Any? = null
)
