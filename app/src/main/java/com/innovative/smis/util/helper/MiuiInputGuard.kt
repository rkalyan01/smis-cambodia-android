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
    private const val TAP_WINDOW_MS = 300L  // Window to detect rapid taps
    private const val SWIPE_THRESHOLD_DP = 10f  // Movement distance to consider it a swipe (not finger drift)
    private const val SAME_POSITION_THRESHOLD_DP = 20f  // Distance to consider taps in "same location" (reduced to avoid blocking drawer swipes)
    private const val BACK_PRESS_DELAY = 500L // Prevent rapid back spam

    private var lastTapTime = 0L
    private var lastTapX = 0f             // Position of last confirmed tap
    private var lastTapY = 0f             // Position of last confirmed tap
    private var lastBackPress = 0L
    private var touchStartX = 0f          // Current gesture starting X
    private var touchStartY = 0f          // Current gesture starting Y
    private var isSwipeGesture = false    // True if movement exceeds threshold

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
        Log.d(TAG, "🔧 $deviceType device — enabling position-based rapid tap guard")
        
        // Calculate thresholds in pixels from DP
        val density = activity.resources.displayMetrics.density
        val swipeThresholdPx = SWIPE_THRESHOLD_DP * density
        val samePositionThresholdPx = SAME_POSITION_THRESHOLD_DP * density

        activity.window.decorView.post {
            activity.window.decorView.setOnTouchListener { _: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val now = SystemClock.elapsedRealtime()
                        
                        // Store starting position of current gesture
                        touchStartX = event.x
                        touchStartY = event.y
                        isSwipeGesture = false
                        
                        // Check if this is a rapid double-tap in the SAME LOCATION
                        // (Different locations = not a double-tap, e.g., drawer swipe from edge)
                        if (now - lastTapTime < TAP_WINDOW_MS) {
                            val distanceFromLastTap = kotlin.math.sqrt(
                                (event.x - lastTapX) * (event.x - lastTapX) +
                                (event.y - lastTapY) * (event.y - lastTapY)
                            )
                            
                            if (distanceFromLastTap < samePositionThresholdPx) {
                                Log.w(TAG, "⚠️ Rapid double-tap blocked (distance: ${distanceFromLastTap.toInt()}px < ${samePositionThresholdPx.toInt()}px)")
                                return@setOnTouchListener true // Consume to prevent black screen
                            } else {
                                Log.d(TAG, "✅ Different location tap allowed (distance: ${distanceFromLastTap.toInt()}px)")
                            }
                        }
                    }
                    
                    MotionEvent.ACTION_MOVE -> {
                        // Calculate movement distance from starting position
                        val deltaX = kotlin.math.abs(event.x - touchStartX)
                        val deltaY = kotlin.math.abs(event.y - touchStartY)
                        
                        // If movement exceeds threshold, it's a swipe (not finger drift)
                        if (!isSwipeGesture && (deltaX > swipeThresholdPx || deltaY > swipeThresholdPx)) {
                            isSwipeGesture = true
                        }
                    }
                    
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val now = SystemClock.elapsedRealtime()
                        
                        if (!isSwipeGesture) {
                            // Tap gesture completed (not a swipe) - record position and time
                            lastTapX = touchStartX
                            lastTapY = touchStartY
                            lastTapTime = now
                            Log.d(TAG, "👆 Tap recorded at (${touchStartX.toInt()}, ${touchStartY.toInt()})")
                        }
                        
                        isSwipeGesture = false
                    }
                }
                false // Let all events through (unless blocked above)
            }
        }
        
        Log.d(TAG, "✅ Position-based tap guard active on $deviceType (same location: ${samePositionThresholdPx.toInt()}px)")
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
