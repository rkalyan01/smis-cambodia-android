package com.innovative.smis.util.helper

import android.content.Context
import android.content.SharedPreferences
import com.innovative.smis.util.constants.Languages

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
    }

    enum class ThemeMode {
        LIGHT, DARK, AUTO
    }

    var selectedLanguage: String
        get() = sharedPreferences.getString(KEY_LANGUAGE, Languages.ENGLISH) ?: Languages.ENGLISH
        set(value) = sharedPreferences.edit().putString(KEY_LANGUAGE, value).apply()

    var themeMode: ThemeMode
        get() {
            val mode = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name
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

    fun getAuthToken(): String? {
        return sharedPreferences.getString("AUTH_TOKEN", null)
    }

    fun setAuthToken(token: String) {
        sharedPreferences.edit().putString("AUTH_TOKEN", token).apply()
    }

    fun saveAuthToken(token: String) {
        setAuthToken(token)
    }

    fun saveUserData(name: String, email: String) {
        sharedPreferences.edit()
            .putString("USER_NAME", name)
            .putString("USER_EMAIL", email)
            .putBoolean("IS_LOGIN", true)
            .apply()
    }
    
    fun saveEtoId(etoId: Int) {
        sharedPreferences.edit()
            .putInt("ETO_ID", etoId)
            .apply()
    }
    
    fun getEtoId(): Int? {
        val etoId = sharedPreferences.getInt("ETO_ID", -1)
        return if (etoId == -1) null else etoId
    }
    
    fun getUserName(): String? {
        return sharedPreferences.getString("USER_NAME", null)
    }
    
    fun getUserEmail(): String? {
        return sharedPreferences.getString("USER_EMAIL", null)
    }
    
    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("IS_LOGIN", false)
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

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun getAllPreferences(): Map<String, *> {
        return sharedPreferences.all
    }
}