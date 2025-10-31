package com.innovative.smis.data.model.response

data class ContainmentStatusResponse(
    val success: Boolean,
    val data: ContainmentData?
)

data class ContainmentData(
    val type_of_storage_tank: String?,
    val other_type_of_storage_tank: String?,
    val storage_tank_connection: String?,
    val other_storage_tank_connection: String?,
    val size_of_storage_tank_m3: String?,
    val construction_year: Int?,
    val accessibility: Boolean?,
    val ever_emptied: Boolean?,
    val last_emptied_year: Int?,
    val road_code: String?,
    val sanitation_customer_id: String?
)