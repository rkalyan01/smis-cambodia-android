package com.innovative.smis.util.ui

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import android.util.Log

private const val TAG = "ClickDebounce"

/**
 * Debounced click modifier that prevents rapid successive clicks
 * 
 * Ignores clicks that occur within [debounceTime] milliseconds of the previous click.
 * This prevents navigation race conditions and black/white screen issues caused by
 * rapid clicking (e.g., back button then drawer item).
 * 
 * @param debounceTime Minimum time in milliseconds between clicks (default: 500ms)
 * @param onClick Callback invoked when click passes debounce check
 */
fun Modifier.clickableDebounced(
    debounceTime: Long = 500L,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    clickable {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastClick = currentTime - lastClickTime
        
        if (timeSinceLastClick > debounceTime) {
            lastClickTime = currentTime
            Log.d(TAG, "✅ Click allowed (${timeSinceLastClick}ms since last)")
            onClick()
        } else {
            Log.d(TAG, "⚠️ Click debounced (only ${timeSinceLastClick}ms since last, need ${debounceTime}ms)")
        }
    }
}
