package com.innovative.smis.util.helper

import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity

/**
 * MIUI/HyperOS black screen prevention utility
 * 
 * Prevents MIUI's DisplayFeatureHal "mistouch prevention" system from triggering
 * black screen overlays during rapid UI interactions.
 * 
 * Usage in MainActivity:
 * ```
 * MiuiInputGuard.attachTapInterceptor(this)
 * ```
 * 
 * Usage in BackHandler:
 * ```
 * if (MiuiInputGuard.shouldAllowBack()) {
 *     // Handle back press
 * }
 * ```
 */
object MiuiInputGuard {

    private const val TAG = "MiuiInputGuard"
    private const val MAX_TAPS = 2          // Block 2nd+ tap burst (MIUI mistouch prevention triggers on double-tap)
    private const val TAP_WINDOW_MS = 300L  // Increased to catch slower double-taps
    private const val BACK_PRESS_DELAY = 500L // Prevent rapid back spam

    private var tapCount = 0
    private var lastTapTime = 0L
    private var lastBackPress = 0L

    /**
     * Check if device is MIUI / HyperOS
     * More reliable than manufacturer string check
     */
    fun isMiuiDevice(): Boolean {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val method = cls.getMethod("get", String::class.java)
            val miui = method.invoke(cls, "ro.miui.ui.version.name") as String
            val hyper = method.invoke(cls, "ro.miui.version.code") as String
            miui.isNotEmpty() || hyper.isNotEmpty()
        } catch (e: Exception) {
            // Fallback to manufacturer check
            val manufacturer = android.os.Build.MANUFACTURER.lowercase()
            manufacturer.contains("xiaomi") || 
            manufacturer.contains("redmi") || 
            manufacturer.contains("poco")
        }
    }

    /**
     * Prevent rapid double-tapping that triggers black screen overlay
     * Attach this in MainActivity.onCreate()
     * 
     * NOTE: Originally for MIUI mistouch prevention, but issue occurs on ALL Android devices
     */
    fun attachTapInterceptor(activity: ComponentActivity) {
        val deviceType = if (isMiuiDevice()) "MIUI/HyperOS" else "Android"
        Log.d(TAG, "🔧 $deviceType device — enabling rapid tap guard for ALL devices")

        activity.window.decorView.post {
            activity.window.decorView.setOnTouchListener { _: View, event: MotionEvent ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val now = SystemClock.elapsedRealtime()

                    tapCount = if (now - lastTapTime < TAP_WINDOW_MS) {
                        tapCount + 1
                    } else {
                        1
                    }
                    lastTapTime = now

                    if (tapCount >= MAX_TAPS) {
                        Log.w(TAG, "⚠️ Rapid taps blocked to prevent MIUI black-screen bug (tap #$tapCount)")
                        return@setOnTouchListener true // Consume event
                    }
                }
                false // Let normal events through
            }
        }
        
        Log.d(TAG, "✅ Rapid tap interception active on $deviceType")
    }

    /**
     * Prevent rapid back spam
     * Call this before handling back navigation
     * 
     * @return true if back press should be allowed, false if it should be blocked
     */
    fun shouldAllowBack(): Boolean {
        val now = SystemClock.elapsedRealtime()
        return if (now - lastBackPress < BACK_PRESS_DELAY) {
            Log.w(TAG, "⛔ Back press blocked (MIUI crash prevention - ${now - lastBackPress}ms since last)")
            false
        } else {
            lastBackPress = now
            true
        }
    }
    
    /**
     * Check if device is MIUI without logging
     * Useful for conditional UI logic
     */
    fun isMiuiDeviceQuiet(): Boolean {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val method = cls.getMethod("get", String::class.java)
            val miui = method.invoke(cls, "ro.miui.ui.version.name") as String
            val hyper = method.invoke(cls, "ro.miui.version.code") as String
            miui.isNotEmpty() || hyper.isNotEmpty()
        } catch (e: Exception) {
            val manufacturer = android.os.Build.MANUFACTURER.lowercase()
            manufacturer.contains("xiaomi") || 
            manufacturer.contains("redmi") || 
            manufacturer.contains("poco")
        }
    }
}
