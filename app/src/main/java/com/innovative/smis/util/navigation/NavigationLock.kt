package com.innovative.smis.util.navigation

import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NavigationLock {
    private var isGuardActive by mutableStateOf(false)
    private var isNavigating by mutableStateOf(false)
    private val mutex = Mutex()
    
    suspend fun activateGuard(durationMs: Long) {
        isGuardActive = true
        Log.d("NavigationLock", "🔒 Navigation guard ACTIVATED for ${durationMs}ms")
        kotlinx.coroutines.delay(durationMs)
        isGuardActive = false
        Log.d("NavigationLock", "🔓 Navigation guard DEACTIVATED")
    }
    
    suspend fun tryNavigate(route: String, navigate: () -> Unit): Boolean {
        return mutex.withLock {
            if (isGuardActive) {
                Log.w("NavigationLock", "❌ Navigation to '$route' BLOCKED - guard active")
                return@withLock false
            }
            
            if (isNavigating) {
                Log.w("NavigationLock", "❌ Navigation to '$route' BLOCKED - navigation in progress")
                return@withLock false
            }
            
            isNavigating = true
            Log.d("NavigationLock", "✅ Navigation to '$route' ALLOWED")
            
            try {
                navigate()
                true
            } finally {
                kotlinx.coroutines.delay(300)
                isNavigating = false
            }
        }
    }
    
    fun isLocked(): Boolean = isGuardActive || isNavigating
}
