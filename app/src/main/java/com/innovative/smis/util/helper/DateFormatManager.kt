package com.innovative.smis.util.helper

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object DateFormatManager {
    
    /**
     * Get the user's preferred date format for display
     */
    fun getDisplayFormat(context: Context): String {
        val preferenceHelper = PreferenceHelper(context)
        return preferenceHelper.dateFormat.pattern
    }
    
    /**
     * Get the user's preferred SimpleDateFormat for display
     */
    fun getDisplayFormatter(context: Context): SimpleDateFormat {
        return SimpleDateFormat(getDisplayFormat(context), Locale.getDefault())
    }
    
    /**
     * Get the API date format (always yyyy-MM-dd)
     */
    fun getApiFormat(): String {
        return "yyyy-MM-dd"
    }
    
    /**
     * Get the API SimpleDateFormat
     */
    fun getApiFormatter(): SimpleDateFormat {
        return SimpleDateFormat(getApiFormat(), Locale.getDefault())
    }
    
    /**
     * Format a timestamp to display format based on user preference
     */
    fun formatTimestampForDisplay(context: Context, timestamp: Long?): String {
        if (timestamp == null) return ""
        return try {
            getDisplayFormatter(context).format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Format a timestamp to API format (yyyy-MM-dd)
     */
    fun formatTimestampForApi(timestamp: Long?): String {
        if (timestamp == null) return ""
        return try {
            getApiFormatter().format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Parse a date string in display format to timestamp
     */
    fun parseDisplayDate(context: Context, dateString: String): Long? {
        if (dateString.isBlank()) return null
        return try {
            getDisplayFormatter(context).parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse a date string in API format to timestamp
     */
    fun parseApiDate(dateString: String): Long? {
        if (dateString.isBlank()) return null
        return try {
            getApiFormatter().parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert API date format to display format
     */
    fun convertApiToDisplay(context: Context, apiDateString: String): String {
        if (apiDateString.isBlank()) return ""
        return try {
            val timestamp = parseApiDate(apiDateString)
            formatTimestampForDisplay(context, timestamp)
        } catch (e: Exception) {
            apiDateString
        }
    }
    
    /**
     * Convert display date format to API format
     */
    fun convertDisplayToApi(context: Context, displayDateString: String): String {
        if (displayDateString.isBlank()) return ""
        return try {
            val timestamp = parseDisplayDate(context, displayDateString)
            formatTimestampForApi(timestamp)
        } catch (e: Exception) {
            displayDateString
        }
    }
    
    /**
     * Get current date in display format
     */
    fun getTodayInDisplayFormat(context: Context): String {
        return formatTimestampForDisplay(context, Date().time)
    }
    
    /**
     * Get current date in API format
     */
    fun getTodayInApiFormat(): String {
        return formatTimestampForApi(Date().time)
    }
}
