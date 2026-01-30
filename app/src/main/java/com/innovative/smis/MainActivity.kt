package com.innovative.smis

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.innovative.smis.ui.base.MyApp
import com.innovative.smis.ui.theme.SMISTheme
import com.innovative.smis.util.permission.PermissionManager
import com.innovative.smis.ui.components.PermissionDialog
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.util.helper.MiuiInputGuard
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔧 CRITICAL FIX: MIUI/HyperOS black screen prevention
        // Uses enhanced MiuiInputGuard utility for comprehensive protection
        if (MiuiInputGuard.isMiuiDevice()) {
            // Disable edge-to-edge to prevent firmware interference
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
            Log.d("MIUI-FIX", "🔧 MIUI/HyperOS detected - disabling edge-to-edge")
        }
        
        // Note: Tap interceptor removed - black screen was a navigation race condition,
        // not a touch event issue. Fixed by using safe navigation and debounced clicks.

        setContent {
            SMISTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionAwareApp()
                }
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context?) {
        val preferenceHelper = newBase?.let { PreferenceHelper(it) }
        val languageCode = preferenceHelper?.selectedLanguage ?: "km"
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(newBase?.resources?.configuration ?: Configuration())
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        super.attachBaseContext(newBase?.createConfigurationContext(config))
    }

    @Composable
    private fun PermissionAwareApp() {
        // Global permission check disabled to allow LoginScreen to handle it
        // with language support and better UI flow.
        MyApp()
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SMISTheme {
        MyApp()
    }
}