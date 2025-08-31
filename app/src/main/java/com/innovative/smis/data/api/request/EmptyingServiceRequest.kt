package com.innovative.smis.data.api.request

data class EmptyingServiceRequest(
    val start_time: String?,
    val end_time: String?,
    val volume_of_sludge: String?,
    val no_of_trips: String?,
    val sludge_type_a: String?,
    val sludge_type_b: String?,
    val location_of_containment: String?,
    val presence_of_pumping_point: String?,
    val other_additional_repairing: String?,
    val extra_payment: String?,
    val receipt_number: String?,
    val comments: String?,
    val receipt_image: String?,
    val picture_of_emptying: String?,
    val eto_id: String?,
    val desludging_vehicle_id: String?,
    val longitude: Double?,
    val latitude: Double?
)