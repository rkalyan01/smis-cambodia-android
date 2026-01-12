package com.innovative.smis.data.model.request

import com.squareup.moshi.Json

data class PostponeRequest(
    @Json(name = "type")
    val type: String,

    @Json(name = "postpone_from")
    val postponeFrom: String? = null,

    @Json(name = "postpone_to")
    val postponeTo: String? = null,

    @Json(name = "prepone_from")
    val preponeFrom: String? = null,

    @Json(name = "prepone_to")
    val preponeTo: String? = null,

    @Json(name = "reason")
    val reason: String,

    @Json(name = "remark")
    val remark: String
)