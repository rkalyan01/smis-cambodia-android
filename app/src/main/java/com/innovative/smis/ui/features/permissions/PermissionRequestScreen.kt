package com.innovative.smis.ui.features.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources

data class PermissionInfo(
    val permission: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val preferenceHelper = PreferenceHelper(context)
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }
    
    val requiredPermissions = listOf(
        PermissionInfo(
            permission = Manifest.permission.CAMERA,
            title = StringResources.getString(StringResources.CAMERA_ACCESS, languageCode),
            description = StringResources.getString(StringResources.TAKE_PHOTOS_DESCRIPTION, languageCode),
            icon = Icons.Default.PhotoCamera
        ),
        PermissionInfo(
            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
            title = StringResources.getString(StringResources.PHOTO_GALLERY_ACCESS, languageCode),
            description = StringResources.getString(StringResources.SELECT_PHOTOS_DESCRIPTION, languageCode),
            icon = Icons.Default.PhotoLibrary
        ),
        PermissionInfo(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            title = StringResources.getString(StringResources.LOCATION_ACCESS, languageCode),
            description = StringResources.getString(StringResources.LOCATION_ACCESS_DESCRIPTION, languageCode),
            icon = Icons.Default.LocationOn
        ),
        PermissionInfo(
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
            title = StringResources.getString(StringResources.APPROXIMATE_LOCATION, languageCode),
            description = StringResources.getString(StringResources.GENERAL_LOCATION_DESCRIPTION, languageCode),
            icon = Icons.Default.MyLocation,
            isRequired = false
        )
    )
    
    var permissionStates by remember {
        mutableStateOf(
            requiredPermissions.associate { permissionInfo ->
                permissionInfo.permission to (ContextCompat.checkSelfPermission(
                    context,
                    permissionInfo.permission
                ) == PackageManager.PERMISSION_GRANTED)
            }
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionStates = permissions
        
        // Check if all required permissions are granted
        val requiredGranted = requiredPermissions
            .filter { it.isRequired }
            .all { permissionStates[it.permission] == true }
            
        if (requiredGranted) {
            // Mark permissions as handled
            preferenceHelper.setBoolean(PrefConstant.PERMISSIONS_REQUESTED, true)
            onPermissionsGranted()
        }
    }
    
    val allRequiredGranted = requiredPermissions
        .filter { it.isRequired }
        .all { permissionStates[it.permission] == true }
    
    LaunchedEffect(allRequiredGranted) {
        if (allRequiredGranted) {
            preferenceHelper.setBoolean(PrefConstant.PERMISSIONS_REQUESTED, true)
            onPermissionsGranted()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Logo/Icon placeholder
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = "SMIS Logo",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to SMIS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sewer Management Information System",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "To provide the best experience, SMIS needs access to the following:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requiredPermissions) { permissionInfo ->
                PermissionCard(
                    permissionInfo = permissionInfo,
                    isGranted = permissionStates[permissionInfo.permission] == true
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Grant Permissions Button
        Button(
            onClick = {
                val permissionsToRequest = requiredPermissions
                    .map { it.permission }
                    .filter { permissionStates[it] != true }
                    .toTypedArray()
                
                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest)
                } else {
                    onPermissionsGranted()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (allRequiredGranted) {
                Text("Continue to App")
            } else {
                Text("Grant Permissions")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Skip button (only if not all required permissions are needed)
        TextButton(
            onClick = {
                preferenceHelper.setBoolean(PrefConstant.PERMISSIONS_REQUESTED, true)
                onPermissionsGranted()
            }
        ) {
            Text("Continue without some permissions")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionCard(
    permissionInfo: PermissionInfo,
    isGranted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isGranted) {
            null
        } else if (permissionInfo.isRequired) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
            )
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = permissionInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = permissionInfo.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (permissionInfo.isRequired) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "*",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                
                Text(
                    text = permissionInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}