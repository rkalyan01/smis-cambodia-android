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
        val context = LocalContext.current
        
        // ✅ FIX: Add specific Loading state to prevent FOUC (Flash of Unverified Content)
        // This prevents MyApp from rendering before permission check completes
        var isCheckingPermissions by remember { mutableStateOf(true) }
        var permissionsGranted by remember { mutableStateOf(false) }
        var showPermissionDialog by remember { mutableStateOf(false) }
        var missingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d("MainActivity", "Permission result received: $permissions")
            val deniedPermissions = permissions.filter { !it.value }.keys
            if (deniedPermissions.isEmpty()) {
                Log.d("MainActivity", "All permissions granted")
                permissionsGranted = true
                showPermissionDialog = false
            } else {
                Log.d("MainActivity", "Some permissions denied: $deniedPermissions")
                permissionsGranted = false
                showPermissionDialog = false // Or keep true to force them
            }
        }

        // ✅ PERFORMANCE: Check permissions on IO thread to avoid main thread blocking
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val hasAllPermissions = PermissionManager.hasAllPermissions(context)
                val missing = if (!hasAllPermissions) {
                    PermissionManager.getMissingPermissions(context)
                } else {
                    emptyList()
                }

                withContext(Dispatchers.Main) {
                    Log.d("MainActivity", "Initial permission check - hasAllPermissions: $hasAllPermissions")
                    if (!hasAllPermissions) {
                        Log.d("MainActivity", "Missing permissions: $missing")
                        missingPermissions = missing
                        showPermissionDialog = true
                        permissionsGranted = false
                    } else {
                        showPermissionDialog = false
                        permissionsGranted = true
                    }
                    // ✅ Only now do we allow UI to render (prevents FOUC and frame skips)
                    isCheckingPermissions = false
                    Log.d("MainActivity", "🚀 Permission check complete - isCheckingPermissions set to false")
                }
            }
        }

        // 1. Render LOADING first (prevents MyApp from initializing too early)
        if (isCheckingPermissions) {
            Log.d("MainActivity", "⏳ Showing loading screen during permission check")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            return
        }

        // 2. Render Dialog if needed
        if (showPermissionDialog && missingPermissions.isNotEmpty() && !permissionsGranted) {
            Log.d("MainActivity", "📋 Showing permission dialog")
            PermissionDialog(
                missingPermissions = missingPermissions,
                onGrantPermissions = {
                    Log.d("MainActivity", "Grant permissions clicked - launching permission request")
                    permissionLauncher.launch(missingPermissions.toTypedArray())
                },
                onDismiss = {
                    Log.d("MainActivity", "Permission dialog dismissed")
                    // If dismissed without granting, user proceeds (logic based on your previous code)
                    showPermissionDialog = false
                }
            )
        }

        // 3. Render Main App ONLY if checking is done AND dialog is not blocking
        if (!showPermissionDialog) {
            Log.d("MainActivity", "✅ Rendering MyApp - permission check complete and dialog not showing")
            MyApp()
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SMISTheme {
        MyApp()
    }
}