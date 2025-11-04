package com.innovative.smis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.innovative.smis.data.api.request.EmptyingServiceRequest

@Entity(tableName = "emptying_service_forms")
data class EmptyingServiceFormEntity(
    @PrimaryKey
    val applicationId: Int,

    // Service Details
    val emptiedDate: Long = System.currentTimeMillis(),
    val startTime: String = "",
    val endTime: String = "",
    val additionalTripRequired: String = "no", // "yes" or "no"

    // Personnel Information
    val sanitationCustomerId: String? = null,
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
    val otherAdditionalRepairing: String = "",
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
    // Separate additional_repairing_id (keys only) from other_additional_repairing (Others text)
    val selectedKeys = additionalRepairingInEmptying.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "Others" }
        .joinToString(",")
    
    val othersText = if (additionalRepairingInEmptying.contains("Others", ignoreCase = true) && otherAdditionalRepairing.isNotEmpty()) {
        otherAdditionalRepairing
    } else {
        null
    }
    
    return EmptyingServiceRequest(
        sanitation_customer_id = sanitationCustomerId,
        start_time = startTime,
        end_time = endTime,
        volume_of_sludge = "3", // Default volume - will be made configurable later
        amount_of_regular_payment_per_trip = regularCost,
        additional_trip_required = additionalTripRequired,
        sludge_type_a = if (sludgeType == "Mixed") "Mixed" else if (sludgeType == "Not Mixed") "Not mixed" else "",
        sludge_type_b = if (sludgeType == "Mixed" && typeOfSludge.isNotEmpty()) typeOfSludge else "",
        location_of_containment = "Around the house", // Default location - will be made configurable
        presence_of_pumping_point = pumpingPointPresence.ifEmpty { null },
        pumping_point_type = if (pumpingPointPresence == "Yes" && pumpingPointType.isNotEmpty()) pumpingPointType else null,
        additional_repairing_id = selectedKeys.takeIf { it.isNotEmpty() },
        other_additional_repairing = othersText,
        extra_payment = extraCost,
        receipt_number = receiptNumber,
        comments = comments,
        receipt_image_base64 = receiptImage,
        picture_of_emptying_base64 = pictureOfEmptying,
        eto_id = "", // Empty - will be filled by repository from PreferenceHelper
        desludging_vehicle_id = desludgingVehicleId,
        lng = longitude,
        lat = latitude,
        service_receiver_name = serviceReceiverName,
        service_receiver_contact = serviceReceiverContact
    )
}
