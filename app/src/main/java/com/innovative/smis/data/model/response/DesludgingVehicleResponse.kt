package com.innovative.smis.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DesludgingVehicleListResponse(
    @Json(name = "eto_id")
    val etoId: String,
    @Json(name = "vehicles")
    val vehicles: List<VehicleResponse>
)

@JsonClass(generateAdapter = true)
data class DesludgingVehicleData(
    @Json(name = "eto_id")
    val etoId: String,
    @Json(name = "vehicles")
    val vehicles: List<VehicleResponse>
)

@JsonClass(generateAdapter = true)
data class VehicleResponse(
    @Json(name = "id")
    val id: Int,
    @Json(name = "license_plate_no")
    val licensePlateNo: String,
    @Json(name = "status")
    val status: String = "active",
    @Json(name = "driver_name")
    val driverName: String? = null,
    @Json(name = "capacity")
    val capacity: String? = null,
    @Json(name = "current_location")
    val currentLocation: String? = null,
    @Json(name = "last_maintenance")
    val lastMaintenance: String? = null,
    @Json(name = "assigned_operator")
    val assignedOperator: String? = null
)

@JsonClass(generateAdapter = true)
data class VehicleStatusUpdateResponse(
    @Json(name = "success")
    val success: Boolean,
    @Json(name = "message")
    val message: String,
    @Json(name = "data")
    val data: VehicleResponse? = null
)