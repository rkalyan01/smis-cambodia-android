package com.innovative.smis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.innovative.smis.data.api.request.EmptyingServiceRequest

@Entity(tableName = "emptying_service_forms")
data class EmptyingServiceFormEntity(
    @PrimaryKey
    val id: String,
    val applicationId: Int,

    // Service Details
    val emptiedDate: Long = System.currentTimeMillis(),
    val startTime: String = "",
    val endTime: String = "",
    val noOfTrips: String = "",

    // Personnel Information
    val applicantName: String = "",
    val applicantContact: String = "",
    val serviceReceiverName: String = "",
    val serviceReceiverContact: String = "",
    val isServiceReceiverSameAsApplicant: Boolean = false,

    // Vehicle and Sludge Information
    val desludgingVehicleId: String = "",
    val sludgeType: String = "", // "Mixed" or "Not Mixed"
    val typeOfSludge: String = "", // When Mixed: "Processing food", "Oil and fat (restaurant)", "Content of fuel"
    val pumpingPointPresence: String = "", // "Yes" or "No"
    val pumpingPointType: String = "", // When Yes: "Cover", "Tube", "Pierce"

    // Service Information
    val freeUnderPBC: Boolean = false,
    val additionalRepairingInEmptying: String = "",
    val regularCost: String = "",
    val extraCost: String = "",

    // Documentation
    val receiptNumber: String = "",
    val receiptImage: String = "",
    val pictureOfEmptying: String = "",
    val comments: String = "",

    // Location
    val longitude: Double? = null,
    val latitude: Double? = null,

    // Metadata
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "DRAFT" // DRAFT, PENDING, FAILED, SYNCED
)

fun EmptyingServiceFormEntity.toApiRequest(): EmptyingServiceRequest {
    return EmptyingServiceRequest(
        start_time = startTime,
        end_time = endTime,
        volume_of_sludge = "3", // Default volume - will be made configurable later
        no_of_trips = noOfTrips,
        sludge_type_a = if (sludgeType == "Mixed") "Mixed" else if (sludgeType == "Not Mixed") "Not mixed" else "",
        sludge_type_b = if (sludgeType == "Mixed" && typeOfSludge.isNotEmpty()) typeOfSludge else "",
        location_of_containment = "Around the house", // Default location - will be made configurable
        presence_of_pumping_point = if (pumpingPointPresence == "Yes") "Yes (Cover, Tube, Pierce)" else "No (need to pierce the tank)",
        other_additional_repairing = additionalRepairingInEmptying,
        extra_payment = extraCost,
        receipt_number = receiptNumber,
        comments = comments,
        receipt_image = receiptImage,
        picture_of_emptying = pictureOfEmptying,
        eto_id = "4", // Default ETO ID - will be made configurable later
        desludging_vehicle_id = desludgingVehicleId,
        longitude = longitude,
        latitude = latitude
    )
}
