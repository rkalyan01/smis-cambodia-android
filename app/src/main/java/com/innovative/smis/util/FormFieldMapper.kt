package com.innovative.smis.util

import com.innovative.smis.R

object FormFieldMapper {
    
    val emptyingFormFields = mapOf(
        "task_details" to R.string.form_task_details,
        "location" to R.string.form_location,
        "address" to R.string.form_address,
        "coordinates" to R.string.form_coordinates,
        "latitude" to R.string.form_latitude,
        "longitude" to R.string.form_longitude,
        "date" to R.string.form_date,
        "time" to R.string.form_time,
        "start_time" to R.string.form_start_time,
        "end_time" to R.string.form_end_time,
        "duration" to R.string.form_duration,
        "worker_name" to R.string.form_worker_name,
        "supervisor" to R.string.form_supervisor,
        "team_size" to R.string.form_team_size,
        "equipment" to R.string.form_equipment,
        "vehicle_number" to R.string.form_vehicle_number,
        "truck_capacity" to R.string.form_truck_capacity,
        "emptying_type" to R.string.form_emptying_type,
        "tank_type" to R.string.form_tank_type,
        "tank_capacity" to R.string.form_tank_capacity,
        "sludge_level" to R.string.form_sludge_level,
        "water_level" to R.string.form_water_level,
        "volume_emptied" to R.string.form_volume_emptied,
        "sludge_quality" to R.string.form_sludge_quality,
        "access_difficulty" to R.string.form_access_difficulty,
        "safety_equipment" to R.string.form_safety_equipment,
        "environmental_conditions" to R.string.form_environmental_conditions,
        "weather" to R.string.form_weather,
        "temperature" to R.string.form_temperature,
        "notes" to R.string.form_notes,
        "additional_comments" to R.string.form_additional_comments,
        "photos" to R.string.form_photos,
        "before_photos" to R.string.form_before_photos,
        "after_photos" to R.string.form_after_photos
    )
    
    val buildingSurveyFormFields = mapOf(
        "building_details" to R.string.form_building_details,
        "building_name" to R.string.form_building_name,
        "building_type" to R.string.form_building_type,
        "number_of_floors" to R.string.form_number_of_floors,
        "total_units" to R.string.form_total_units,
        "occupied_units" to R.string.form_occupied_units,
        "construction_year" to R.string.form_construction_year,
        "building_area" to R.string.form_building_area,
        "compound_area" to R.string.form_compound_area,
        "sanitation_system" to R.string.form_sanitation_system,
        "toilet_type" to R.string.form_toilet_type,
        "septic_tank" to R.string.form_septic_tank,
        "tank_location" to R.string.form_tank_location,
        "tank_material" to R.string.form_tank_material,
        "tank_condition" to R.string.form_tank_condition,
        "last_emptied" to R.string.form_last_emptied,
        "emptying_frequency" to R.string.form_emptying_frequency,
        "access_road" to R.string.form_access_road,
        "road_width" to R.string.form_road_width,
        "distance_to_tank" to R.string.form_distance_to_tank,
        "contact_info" to R.string.form_contact_info,
        "owner_name" to R.string.form_owner_name,
        "phone_number" to R.string.form_phone_number,
        "email" to R.string.form_email,
        "contact_person" to R.string.form_contact_person,
        "alternate_phone" to R.string.form_alternate_phone
    )
    
    val buildingTypeOptions = listOf(
        "residential" to R.string.building_residential,
        "commercial" to R.string.building_commercial,
        "institutional" to R.string.building_institutional,
        "mixed_use" to R.string.building_mixed_use
    )
    
    val tankTypeOptions = listOf(
        "septic" to R.string.tank_septic,
        "holding" to R.string.tank_holding,
        "pit_latrine" to R.string.tank_pit_latrine,
        "other" to R.string.tank_other
    )
    
    val qualityLevelOptions = listOf(
        "good" to R.string.quality_good,
        "fair" to R.string.quality_fair,
        "poor" to R.string.quality_poor,
        "very_poor" to R.string.quality_very_poor
    )
    
    val formActionLabels = mapOf(
        "submit" to R.string.form_submit,
        "save_draft" to R.string.form_save_draft,
        "clear" to R.string.form_clear,
        "reset" to R.string.form_reset,
        "next" to R.string.form_next,
        "previous" to R.string.form_previous,
        "finish" to R.string.form_finish
    )
    
    val validationMessages = mapOf(
        "required" to R.string.validation_required,
        "invalid_phone" to R.string.validation_invalid_phone,
        "invalid_email" to R.string.validation_invalid_email,
        "invalid_number" to R.string.validation_invalid_number,
        "invalid_date" to R.string.validation_invalid_date,
        "min_length" to R.string.validation_min_length,
        "max_length" to R.string.validation_max_length
    )

    fun getFieldLabelResId(fieldId: String): Int? {
        return emptyingFormFields[fieldId] ?: buildingSurveyFormFields[fieldId]
    }

    fun getActionLabelResId(actionId: String): Int? {
        return formActionLabels[actionId]
    }

    fun getValidationMessageResId(validationType: String): Int? {
        return validationMessages[validationType]
    }
}