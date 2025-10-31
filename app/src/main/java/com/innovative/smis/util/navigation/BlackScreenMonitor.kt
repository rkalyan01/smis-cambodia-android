package com.innovative.smis.util.navigation

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Black Screen Monitor - Reactive watchdog system that detects when screens fail to render
 * and automatically triggers recovery by navigating to dashboard or reloading the app.
 *
 * How it works:
 * 1. Listens to navigation destination changes
 * 2. Starts a timeout timer (3 seconds) for each new destination
 * 3. Waits for screen "heartbeat" (confirmation that composition rendered)
 * 4. If no heartbeat received within timeout = BLACK SCREEN DETECTED
 * 5. Triggers tiered recovery: navigate to dashboard → reload app if that fails
 */
class BlackScreenMonitor(
    private val scope: CoroutineScope,
    private val timeout: Long = 3000L // 3 seconds to render
) {
    companion object {
        private const val TAG = "BlackScreenMonitor"
        private const val MAX_RECOVERY_ATTEMPTS = 3
        private const val COOLDOWN_PERIOD = 5000L // 5 seconds cooldown between recovery attempts
    }

    // Current state
    private val _state = MutableStateFlow<MonitorState>(MonitorState.IDLE)
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    // Recovery tracking
    private var recoveryAttempts = 0
    private var lastRecoveryTime = 0L
    private var currentDestination: String? = null
    private var timeoutJob: Job? = null

    // Recovery callbacks
    var onNavigateToRecovery: (() -> Unit)? = null
    var onEscalateToAppReload: (() -> Unit)? = null

    sealed class MonitorState {
        object IDLE : MonitorState()
        data class MONITORING(val destination: String) : MonitorState()
        data class BLACK_SCREEN_DETECTED(val destination: String) : MonitorState()
        data class RECOVERING(val attempt: Int) : MonitorState()
        object ESCALATING : MonitorState()
    }

    /**
     * Called when NavController navigates to a new destination
     */
    fun onDestinationChanged(route: String?) {
        if (route == null) return

        Log.d(TAG, "📍 Navigation to: $route")
        currentDestination = route

        // Cancel any existing timeout
        timeoutJob?.cancel()

        // Update state
        _state.value = MonitorState.MONITORING(route)

        // Start timeout watchdog
        timeoutJob = scope.launch {
            delay(timeout)

            // Timeout reached without heartbeat = BLACK SCREEN!
            Log.e(TAG, "⚠️ BLACK SCREEN DETECTED on route: $route (no heartbeat received)")
            _state.value = MonitorState.BLACK_SCREEN_DETECTED(route)

            // Trigger recovery
            attemptRecovery()
        }
    }

    /**
     * Called by each screen to confirm it rendered successfully
     * This is the "heartbeat" that prevents black screen detection
     */
    fun confirmScreenRendered(route: String) {
        if (route == currentDestination) {
            Log.d(TAG, "💚 Heartbeat received from: $route")
            
            // Cancel timeout - screen rendered successfully
            timeoutJob?.cancel()
            _state.value = MonitorState.IDLE

            // Reset recovery attempts on successful render
            recoveryAttempts = 0
        }
    }

    /**
     * Tiered recovery system
     */
    private fun attemptRecovery() {
        val now = System.currentTimeMillis()

        // Check cooldown to prevent recovery loops
        if (now - lastRecoveryTime < COOLDOWN_PERIOD) {
            Log.w(TAG, "🚫 Recovery on cooldown, skipping")
            return
        }

        recoveryAttempts++
        lastRecoveryTime = now

        Log.w(TAG, "🔧 Attempting recovery #$recoveryAttempts")
        _state.value = MonitorState.RECOVERING(recoveryAttempts)

        if (recoveryAttempts < MAX_RECOVERY_ATTEMPTS) {
            // Tier 1: Navigate back to dashboard
            Log.i(TAG, "✅ Recovery Tier 1: Navigating to dashboard")
            onNavigateToRecovery?.invoke()
        } else {
            // Tier 2: Reload entire app
            Log.e(TAG, "🔄 Recovery Tier 2: Reloading app (max attempts reached)")
            _state.value = MonitorState.ESCALATING
            onEscalateToAppReload?.invoke()
        }
    }

    /**
     * Reset recovery attempts (called when user successfully navigates manually)
     */
    fun reset() {
        Log.d(TAG, "🔄 Monitor reset")
        recoveryAttempts = 0
        timeoutJob?.cancel()
        _state.value = MonitorState.IDLE
    }
}
