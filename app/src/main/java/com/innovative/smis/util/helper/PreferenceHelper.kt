package com.innovative.smis.util.helper

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smis_prefs", Context.MODE_PRIVATE)

    fun setValue(key: String, value: Any) {
        with(prefs.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                else -> throw IllegalArgumentException("Unsupported type")
            }
            commit() // or commit() for immediate saving
        }
    }

    fun getValue(key: String, default: Any): Any? {
        return when (default) {
            is Boolean -> prefs.getBoolean(key, default)
            is String -> prefs.getString(key, default)
            is Int -> prefs.getInt(key, default)
            is Long -> prefs.getLong(key, default)
            is Float -> prefs.getFloat(key, default)
            else -> null
        }
    }
}
