package com.innovative.smis.util.session

import android.util.Log
import com.innovative.smis.data.local.database.SMISDatabase
import com.innovative.smis.util.helper.PreferenceHelper

/**
 * Centralized manager for clearing user session data on login or logout
 * Clears all cached user-specific data while preserving device-level settings
 */
class SessionResetManager(
    private val database: SMISDatabase,
    private val preferenceHelper: PreferenceHelper
) {
    
    /**
     * Clears all user-scoped data when switching users or logging in
     * Preserves: Language preferences, date format, and offline map tiles
     * Clears: Auth data, cached applications, drafts, sync queues, surveys
     */
    suspend fun clearUserSessionData() {
        Log.d(TAG, "Starting session data clearance...")
        
        // 1. Clear Database (Best Effort)
        try {
            database.clearAllTables()
            Log.d(TAG, "✓ Cleared all Room database tables")
        } catch (e: Exception) {
            // Log error but proceed - do NOT block login because of DB clear failure
            // This can happen on some devices due to file locking or foreign key issues
            Log.e(TAG, "⚠️ Failed to clear database tables (proceeding anyway): ${e.message}", e)
        }
        
        // 2. Clear Preferences (Critical)
        try {
            // Clear session-specific preferences (token, user data)
            preferenceHelper.clearSessionOnly()
            Log.d(TAG, "✓ Cleared session preferences")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to clear preferences: ${e.message}", e)
            // Still don't throw - AuthRepository will overwrite critical prefs (token/user) anyway
        }
        
        Log.d(TAG, "Session data clearance completed (Best Effort)")
    }
    
    /**
     * Complete wipe including device settings - used for logout
     */
    suspend fun clearAllData() {
        try {
            Log.d(TAG, "Starting complete data clearance...")
            
            // Clear all database tables
            database.clearAllTables()
            Log.d(TAG, "✓ Cleared all Room database tables")
            
            // Clear ALL preferences including device settings
            preferenceHelper.clearAll()
            Log.d(TAG, "✓ Cleared all preferences")
            
            Log.d(TAG, "Complete data clearance finished")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all data: ${e.message}", e)
            throw SessionResetException("Failed to clear all data: ${e.message}", e)
        }
    }
    
    companion object {
        private const val TAG = "SessionResetManager"
    }
}

/**
 * Exception thrown when session reset fails
 */
class SessionResetException(message: String, cause: Throwable? = null) : Exception(message, cause)
