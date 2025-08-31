package com.innovative.smis.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EmptyingForm(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "applicationId")
    val applicationId: String,
    
    @Json(name = "stepName")
    val stepName: String,
    
    @Json(name = "formData")
    val formData: String?,
    
    @Json(name = "submittedBy")
    val submittedBy: String,
    
    @Json(name = "submissionDate")
    val submissionDate: String,
    
    @Json(name = "createdAt")
    val createdAt: Long
)