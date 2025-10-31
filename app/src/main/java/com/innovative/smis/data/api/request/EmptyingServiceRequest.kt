package com.innovative.smis.data.api.request

data class EmptyingServiceRequest(
    val sanitation_customer_id: String?,
    val start_time: String?,
    val end_time: String?,
    val volume_of_sludge: String?,
    val amount_of_regular_payment_per_trip: String?,
    val additional_trip_required: String?,
    val sludge_type_a: String?,
    val sludge_type_b: String?,
    val location_of_containment: String?,
    val presence_of_pumping_point: String?,
    val pumping_point_type: String?,
    val additional_repairing_id: String?,
    val other_additional_repairing: String?,
    val extra_payment: String?,
    val receipt_number: String?,
    val comments: String?,
    val receipt_image: String?,
    val picture_of_emptying: String?,
    val eto_id: String?,
    val desludging_vehicle_id: String?,
    val lng: Double?,
    val lat: Double?,
    val service_receiver_name: String?,
    val service_receiver_contact: String?
)