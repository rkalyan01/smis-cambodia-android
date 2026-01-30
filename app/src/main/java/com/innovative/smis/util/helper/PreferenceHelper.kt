package com.innovative.smis.util.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.innovative.smis.R
import com.innovative.smis.util.constants.Languages
import com.innovative.smis.util.constants.PrefConstant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferenceHelper(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "smis_preferences"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_OFFLINE_MODE = "offline_mode"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_CACHE_SIZE_LIMIT = "cache_size_limit"
        private const val KEY_DATE_FORMAT = "date_format"
    }

    enum class ThemeMode {
        LIGHT, DARK, AUTO
    }

    enum class DateFormat(val pattern: String, @StringRes val labelResId: Int) {
        DD_MM_YYYY("dd-MM-yyyy", R.string.date_format_dd_mm_yyyy),
        MM_DD_YYYY("MM-dd-yyyy", R.string.date_format_mm_dd_yyyy),
        YYYY_MM_DD("yyyy-MM-dd", R.string.date_format_yyyy_mm_dd);

        fun getDisplayName(context: Context): String {
            val label = context.getString(labelResId)
            val currentDate = SimpleDateFormat(pattern, Locale.US).format(Date())
            return "$label ($currentDate)"
        }
    }

    var selectedLanguage: String
        get() = sharedPreferences.getString(KEY_LANGUAGE, Languages.KHMER) ?: Languages.KHMER
        set(value) = sharedPreferences.edit().putString(KEY_LANGUAGE, value).apply()

    var themeMode: ThemeMode
        get() {
            val mode = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
            return ThemeMode.valueOf(mode)
        }
        set(value) = sharedPreferences.edit().putString(KEY_THEME_MODE, value.name).apply()

    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var isOfflineModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_OFFLINE_MODE, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_OFFLINE_MODE, value).apply()

    var isAutoSyncEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    var cacheSizeLimit: Float
        get() = sharedPreferences.getFloat(KEY_CACHE_SIZE_LIMIT, 500f)
        set(value) = sharedPreferences.edit().putFloat(KEY_CACHE_SIZE_LIMIT, value).apply()

    var dateFormat: DateFormat
        get() {
            val format = sharedPreferences.getString(KEY_DATE_FORMAT, DateFormat.YYYY_MM_DD.name)
                ?: DateFormat.YYYY_MM_DD.name
            return try {
                DateFormat.valueOf(format)
            } catch (e: Exception) {
                DateFormat.YYYY_MM_DD
            }
        }
        set(value) = sharedPreferences.edit().putString(KEY_DATE_FORMAT, value.name).apply()

    fun getAuthToken(): String? {
        return sharedPreferences.getString(PrefConstant.AUTH_TOKEN, null)
    }

    fun setAuthToken(token: String) {
        sharedPreferences.edit().putString(PrefConstant.AUTH_TOKEN, token).apply()
    }

    fun saveAuthToken(token: String) {
        setAuthToken(token)
    }

    fun saveUserData(name: String, email: String) {
        sharedPreferences.edit()
            .putString(PrefConstant.USER_NAME, name)
            .putString(PrefConstant.USER_EMAIL, email)
            .putBoolean(PrefConstant.IS_LOGIN, true)
            .apply()
    }

    fun saveEtoId(etoId: Int) {
        sharedPreferences.edit()
            .putInt("eto_id", etoId)
            .apply()
    }

    fun getEtoId(): Int? {
        val etoId = sharedPreferences.getInt("eto_id", -1)
        return if (etoId == -1) null else etoId
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(PrefConstant.USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(PrefConstant.USER_EMAIL, null)
    }

    fun saveUserRoles(roles: List<String>) {
        val rolesString = roles.joinToString(",")
        sharedPreferences.edit().putString(PrefConstant.USER_ROLE, rolesString).apply()
    }

    fun getUserRoles(): List<String> {
        val rolesString = sharedPreferences.getString(PrefConstant.USER_ROLE, "") ?: ""
        return if (rolesString.isNotEmpty()) {
            rolesString.split(",").map { it.trim() }
        } else {
            emptyList()
        }
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PrefConstant.IS_LOGIN, false)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun setString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun setInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun getUserPermissionsMap(): Map<String, Boolean> {
        val permissionsJson = getString(com.innovative.smis.util.constants.PrefConstant.USER_PERMISSIONS, "") ?: ""
        if (permissionsJson.isEmpty()) return emptyMap()

        val permissions = mutableMapOf<String, Boolean>()
        try {
            val cleanStr = permissionsJson.trim()
            if (cleanStr.contains("UserPermission(")) {
                val start = cleanStr.indexOf("UserPermission(") + "UserPermission(".length
                val end = cleanStr.lastIndexOf(")")
                if (start < end) {
                    val content = cleanStr.substring(start, end)
                    content.split(",").forEach { pair ->
                        val parts = pair.trim().split("=", limit = 2)
                        if (parts.size == 2) {
                            val fieldName = parts[0].trim()
                            val value = parts[1].trim().toBooleanStrictOrNull() ?: false
                            val permissionName = when (fieldName) {
                                "viewMap" -> "View Map"
                                "editBuildingSurvey" -> "Edit Building Survey"
                                "emptyingScheduling" -> "Emtying Scheduling"
                                "sitePreparation" -> "Site Preparation"
                                "emptying" -> "Emptying Service"
                                else -> null
                            }
                            if (permissionName != null) permissions[permissionName] = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PreferenceHelper", "Permission parsing error: ${e.message}")
        }
        return permissions
    }

    /**
     * Clear all session-specific data (auth token, user info)
     * Preserves device-level settings (language, theme, date format, etc.)
     */
    fun clearSessionOnly() {
        sharedPreferences.edit()
            .remove(PrefConstant.AUTH_TOKEN)
            .remove(PrefConstant.USER_NAME)
            .remove(PrefConstant.USER_EMAIL)
            .remove(PrefConstant.IS_LOGIN)
            .remove("eto_id")
            .remove(PrefConstant.USER_ROLE)
            .apply()
    }

    /**
     * Clear ALL preferences including device settings
     * Use this for complete logout
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun getAllPreferences(): Map<String, *> {
        return sharedPreferences.all
    }
}