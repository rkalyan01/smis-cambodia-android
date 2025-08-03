package com.innovative.smis.data.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Represents the entire JSON response from the /login API endpoint.
 * Using Serializable allows you to pass this object between different parts of your app,
 * for example, as a navigation argument.
 */
data class LoginResponse(
    @SerializedName("status")
    val status: Boolean?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("token")
    val token: String?,

    @SerializedName("data")
    val data: UserData?
) : Serializable

/**
 * Represents the nested "data" object within the login response.
 */
data class UserData(
    @SerializedName("name")
    val name: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("permissions")
    val permissions: List<Permission>?
) : Serializable

/**
 * Represents a single permission object within the "permissions" list.
 * The @SerializedName annotation is used here because the key "View Map"
 * contains a space, which is not a valid Kotlin identifier.
 */
data class Permission(
    @SerializedName("View Map")
    val viewMap: Boolean?,

    @SerializedName("Edit Building Survey")
    val editBuildingSurvey: Boolean?
) : Serializable
