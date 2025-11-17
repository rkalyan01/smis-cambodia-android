package com.innovative.smis.util.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import android.util.Log

private const val TAG = "NavigationSafe"

/**
 * Safe navigation extension that prevents navigation race conditions
 * 
 * Only navigates if:
 * 1. Current destination lifecycle is RESUMED (not transitioning)
 * 2. Not already on the destination route (prevents double-tap navigation)
 * 
 * This prevents black/white screen crashes caused by:
 * - Navigating while the current screen is being destroyed (e.g., after back button press)
 * - Rapid double-clicking the same menu item
 * - Main thread freezes during navigation transitions
 * 
 * @param route The destination route
 */
fun NavController.navigateSafe(route: String) {
    val currentState = currentBackStackEntry?.lifecycle?.currentState
    val currentRoute = currentDestination?.route
    
    // Check if already on this route (prevent double navigation)
    if (currentRoute == route) {
        Log.d(TAG, "⚠️ Navigation blocked - already on route: $route")
        return
    }
    
    // Check if lifecycle is stable
    if (currentState == Lifecycle.State.RESUMED) {
        Log.d(TAG, "✅ Safe navigation: $currentRoute → $route")
        navigate(route) {
            popUpTo(graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        Log.w(TAG, "⚠️ Navigation blocked - lifecycle not RESUMED (current: $currentState, from: $currentRoute, to: $route)")
    }
}
