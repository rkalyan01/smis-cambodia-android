package com.innovative.smis.ui.features.permissions

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.innovative.smis.R

@Composable
fun NewPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onLanguageClick: () -> Unit
) {
    val permissionList = remember {
        buildList {
            add(
                PermissionItem(
                    title = R.string.permission_camera_access,
                    description = R.string.permission_camera_description,
                    icon = Icons.Default.CameraAlt
                )
            )
            add(
                PermissionItem(
                    title = R.string.permission_location_precise,
                    description = R.string.permission_location_precise_description,
                    icon = Icons.Default.LocationOn
                )
            )
            add(
                PermissionItem(
                    title = R.string.permission_location_approximate,
                    description = R.string.permission_location_approximate_description,
                    icon = Icons.Default.LocationOn
                )
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(
                    PermissionItem(
                        title = R.string.permission_selected_photos,
                        description = R.string.permission_selected_photos_description,
                        icon = Icons.Default.Security
                    )
                )
                add(
                    PermissionItem(
                        title = R.string.permission_photos_media,
                        description = R.string.permission_photos_media_description,
                        icon = Icons.Default.PhotoLibrary
                    )
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    PermissionItem(
                        title = R.string.permission_photos_media,
                        description = R.string.permission_photos_media_description,
                        icon = Icons.Default.PhotoLibrary
                    )
                )
            } else {
                add(
                    PermissionItem(
                        title = R.string.permission_photo_gallery,
                        description = R.string.permission_photos_media_description,
                        icon = Icons.Default.PhotoLibrary
                    )
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C2C2E)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Explicit Top Row for Language
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, end = 16.dp)
                        .zIndex(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onLanguageClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3E3E42),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Manual text "Language" if resource fails, for safety
                        Text(
                            text = stringResource(R.string.lbl_language_switcher_text), 
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                
                // 2. Shield Icon 
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF4DB6AC),
                        modifier = Modifier.size(64.dp)
                    )
                }

                // Main Content Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Take remaining space
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Title
                    Text(
                        text = stringResource(R.string.label_configuration),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Subtitle
                    Text(
                        text = stringResource(R.string.label_configuration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Permission List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(permissionList) { item ->
                            PermissionItemRow(item)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Buttons
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4DB6AC)
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_grant_permission),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4DB6AC)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_later),
                            color = Color(0xFF4DB6AC)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PermissionItemRow(item: PermissionItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color(0xFF4DB6AC),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(item.description),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

private data class PermissionItem(
    val title: Int,
    val description: Int,
    val icon: ImageVector
)
