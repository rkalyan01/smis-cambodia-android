package com.innovative.smis.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VehicleStatusUpdateRequest(
    @Json(name = "status")
    val status: String // 'active', 'under-maintenance', 'inactive'
)