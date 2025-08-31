package com.innovative.smis

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.innovative.smis.ui.base.MyApp
import com.innovative.smis.ui.theme.SMISTheme
import com.innovative.smis.util.permission.PermissionManager
import com.innovative.smis.ui.components.PermissionDialog

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    @Composable
    private fun PermissionAwareApp() {
        val context = LocalContext.current
        var permissionsGranted by remember { mutableStateOf(false) }
        var showPermissionDialog by remember { mutableStateOf(false) }
        var missingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

        // Create permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d("MainActivity", "Permission result received: $permissions")
            // Handle permission results
            val deniedPermissions = permissions.filter { !it.value }.keys
            if (deniedPermissions.isEmpty()) {
                Log.d("MainActivity", "All permissions granted")
                // All permissions granted - refresh permission state
                permissionsGranted = true
                showPermissionDialog = false
            } else {
                Log.d("MainActivity", "Some permissions denied: $deniedPermissions")
                // Some permissions denied - show dialog again if needed
                permissionsGranted = false
                showPermissionDialog = false
            }
        }

        // ✅ PERFORMANCE: Check permissions on IO thread to avoid main thread blocking
        LaunchedEffect(Unit) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val hasAllPermissions = PermissionManager.hasAllPermissions(context)
                val missing = if (!hasAllPermissions) {
                    PermissionManager.getMissingPermissions(context)
                } else {
                    emptyList()
                }

                // Switch back to main thread to update UI
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
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
                }
            }
        }

        // Show permission dialog if needed
        if (showPermissionDialog && missingPermissions.isNotEmpty() && !permissionsGranted) {
            Log.d("MainActivity", "Showing permission dialog")
            PermissionDialog(
                missingPermissions = missingPermissions,
                onGrantPermissions = {
                    Log.d("MainActivity", "Grant permissions clicked - launching permission request")
                    permissionLauncher.launch(missingPermissions.toTypedArray())
                },
                onDismiss = {
                    Log.d("MainActivity", "Permission dialog dismissed")
                    showPermissionDialog = false
                }
            )
        }

        // Main App - only show if permissions are granted or dialog is dismissed
        if (permissionsGranted || !showPermissionDialog) {
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