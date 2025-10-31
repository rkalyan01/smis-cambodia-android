package com.innovative.smis.util.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import android.util.Log

/**
 * Screen Heartbeat - Confirms that a composable screen has rendered successfully
 * 
 * Usage: Add this to the top of every screen composable:
 * ```
 * ScreenHeartbeat(route = "dashboard", monitor = blackScreenMonitor)
 * ```
 */
@Composable
fun ScreenHeartbeat(
    route: String,
    monitor: BlackScreenMonitor?
) {
    if (monitor == null) return

    // Confirm screen rendered on first composition
    LaunchedEffect(route) {
        Log.d("ScreenHeartbeat", "💚 Sending heartbeat for: $route")
        monitor.confirmScreenRendered(route)
    }

    // Log when screen is disposed
    DisposableEffect(route) {
        onDispose {
            Log.d("ScreenHeartbeat", "👋 Screen disposed: $route")
        }
    }
}
