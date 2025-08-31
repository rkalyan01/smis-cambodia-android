package com.innovative.smis.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContainmentRequest(
    @Json(name = "sanitation_customer_id") val sanitation_customer_id: String,
    @Json(name = "type_of_storage_tank") val type_of_storage_tank: String?,
    @Json(name = "other_type_of_storage_tank") val other_type_of_storage_tank: String?,
    @Json(name = "storage_tank_connection") val storage_tank_connection: String?,
    @Json(name = "other_storage_tank_connection") val other_storage_tank_connection: String?,
    @Json(name = "size_of_storage_tank_m3") val size_of_storage_tank_m3: String?,
    @Json(name = "construction_year") val construction_year: String?,
    @Json(name = "accessibility") val accessibility: String?,
    @Json(name = "ever_emptied") val ever_emptied: String?,
    @Json(name = "last_emptied_year") val last_emptied_year: String?
)