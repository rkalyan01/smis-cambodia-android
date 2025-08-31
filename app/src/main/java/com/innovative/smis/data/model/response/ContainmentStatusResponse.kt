package com.innovative.smis.data.model.response

data class ContainmentStatusResponse(
    val success: Boolean,
    val data: ContainmentData?
)

data class ContainmentData(
    val id: String?,
    val type_of_storage_tank: String?,
    val other_type_of_storage_tank: String?,
    val storage_tank_connection: String?,
    val other_storage_tank_connection: String?,
    val size_of_storage_tank_m3: String?,
    val construction_year: String?,
    val accessibility: String?,
    val ever_emptied: String?,
    val last_emptied_year: String?
)