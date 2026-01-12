package com.innovative.smis.ui.features.permissions

import com.innovative.smis.R

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.res.stringResource
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.helper.PreferenceHelper

data class PermissionInfo(
    val permission: String,
    val titleRes: Int,
    val descriptionRes: Int,
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
    
    // Build permission list based on Android version
    val requiredPermissions = remember {
        buildList {
            // Camera permission - always required
            add(
                PermissionInfo(
                    permission = Manifest.permission.CAMERA,
                    titleRes = R.string.permission_camera_access,
                    descriptionRes = R.string.permission_camera_description,
                    icon = Icons.Default.PhotoCamera,
                    isRequired = true
                )
            )
            
            // Photo/Media permissions - version specific
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ uses READ_MEDIA_IMAGES
                add(
                    PermissionInfo(
                        permission = Manifest.permission.READ_MEDIA_IMAGES,
                        titleRes = R.string.permission_photo_gallery,
                        descriptionRes = R.string.permission_photo_gallery_description,
                        icon = Icons.Default.PhotoLibrary,
                        isRequired = true
                    )
                )
            } else {
                // Android 12 and below uses READ_EXTERNAL_STORAGE
                add(
                    PermissionInfo(
                        permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                        titleRes = R.string.permission_photo_gallery,
                        descriptionRes = R.string.permission_photo_gallery_description,
                        icon = Icons.Default.PhotoLibrary,
                        isRequired = true
                    )
                )
            }
            
            // Location permission - always required
            add(
                PermissionInfo(
                    permission = Manifest.permission.ACCESS_FINE_LOCATION,
                    titleRes = R.string.permission_location,
                    descriptionRes = R.string.permission_location_description,
                    icon = Icons.Default.LocationOn,
                    isRequired = true
                )
            )
        }
    }
    
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
            contentDescription = stringResource(R.string.cd_app_logo),
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.permission_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.permission_app_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.permission_intro_message),
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
                    preferenceHelper.setBoolean(PrefConstant.PERMISSIONS_REQUESTED, true)
                    onPermissionsGranted()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = allRequiredGranted || permissionStates.values.any { !it }
        ) {
            Text(
                if (allRequiredGranted) stringResource(R.string.action_continue_to_app) else stringResource(R.string.action_grant_all_permissions)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
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
        border = if (!isGranted) {
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
                Text(
                    text = stringResource(permissionInfo.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = stringResource(permissionInfo.descriptionRes),
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
