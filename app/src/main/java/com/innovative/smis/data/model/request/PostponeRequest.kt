package com.innovative.smis.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostponeRequest(
    @Json(name = "postpone_from") val postponeFrom: String,
    @Json(name = "postpone_until") val postponeUntil: String,
    @Json(name = "reason") val reason: String,
    @Json(name = "remark") val remark: String
)
