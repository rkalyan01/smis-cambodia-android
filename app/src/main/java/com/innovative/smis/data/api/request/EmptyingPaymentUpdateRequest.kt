package com.innovative.smis.data.api.request

data class EmptyingPaymentUpdateRequest(
    val extra_payment: String?,
    val receipt_number: String?,
    val comments: String?,
    val receipt_image_base64: String?,
    val picture_of_emptying_base64: String?
)
