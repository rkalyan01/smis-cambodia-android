package com.innovative.smis.util.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {

    /**
     * Dynamically determines the required permissions based on the Android version.
     * For Android 13 (API 33) and above, it uses the granular READ_MEDIA_IMAGES.
     * For older versions, it falls back to READ_EXTERNAL_STORAGE.
     */
    val REQUIRED_PERMISSIONS: Array<String>
        get() {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
            )

            // ADDED: Logic for Android 14 (API 34) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Request this permission to handle the "Select Photos" option gracefully.
                permissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13 (API 33)
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                // For Android 12 and older
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            return permissions.toTypedArray()
        }

    const val PERMISSION_REQUEST_CODE = 1001

    /**
     * Check if all required permissions are granted.
     */
    fun hasAllPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get a list of permissions that have not been granted.
     */
    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request all missing permissions from the user.
     */
    fun requestAllPermissions(activity: Activity) {
        val missingPermissions = getMissingPermissions(activity)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Check if a specific permission is granted.
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get user-friendly permission names for display in the UI.
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED -> "Selected Photos"
            Manifest.permission.READ_MEDIA_IMAGES -> "Photos & Media"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage (Read)"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Location (Precise)"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Location (Approximate)"
            Manifest.permission.INTERNET -> "Internet"
            Manifest.permission.ACCESS_NETWORK_STATE -> "Network State"
            else -> permission.substringAfterLast('.')
        }
    }

    /**
     * Get detailed permission descriptions for user understanding.
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Required to capture photos for service documentation"
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED -> "Allows access to only the photos you select"
            Manifest.permission.READ_MEDIA_IMAGES -> "Required to access photos from your gallery"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Required to access photos from your gallery"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Required to record precise location data for services"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Required to record approximate location data"
            Manifest.permission.INTERNET -> "Required to sync data with the server"
            Manifest.permission.ACCESS_NETWORK_STATE -> "Required to check network connectivity"
            else -> "Required for app functionality"
        }
    }
}