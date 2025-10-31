package com.innovative.smis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class DrawerItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val route: String,
    val requiredPermission: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawer(
    navController: NavController,
    topLevelNavController: NavController,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    content: @Composable (onMenuClick: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val preferenceHelper = PreferenceHelper(context)
    val scope = rememberCoroutineScope()

    // Simple click debouncing to prevent accidental double-clicks
    var lastMenuClickTime by remember { mutableLongStateOf(0L) }

    // Load user info and permissions immediately (synchronous, fast)
    // This prevents black screen by ensuring data is always available
    val initialUserName = remember { 
        try {
            preferenceHelper.getString(PrefConstant.USER_NAME, "User") ?: "User"
        } catch (e: Exception) {
            android.util.Log.e("NavigationDrawer", "Error loading userName: ${e.message}", e)
            "User"
        }
    }
    val initialUserEmail = remember { 
        try {
            preferenceHelper.getString(PrefConstant.USER_EMAIL, "") ?: ""
        } catch (e: Exception) {
            android.util.Log.e("NavigationDrawer", "Error loading userEmail: ${e.message}", e)
            ""
        }
    }
    val initialPermissions = remember { 
        try {
            getUserPermissions(preferenceHelper)
        } catch (e: Exception) {
            android.util.Log.e("NavigationDrawer", "Error loading permissions: ${e.message}", e)
            emptyMap()
        }
    }
    val initialLanguage = remember { 
        try {
            preferenceHelper.getString(PrefConstant.CURRENT_LANGUAGE, "English") ?: "English"
        } catch (e: Exception) {
            android.util.Log.e("NavigationDrawer", "Error loading language: ${e.message}", e)
            "English"
        }
    }
    val initialLanguageCode = remember { 
        try {
            LocalizationManager.getLanguageCode(initialLanguage)
        } catch (e: Exception) {
            android.util.Log.e("NavigationDrawer", "Error getting language code: ${e.message}", e)
            "en"
        }
    }
    
    var userName by remember { mutableStateOf(initialUserName) }
    var userEmail by remember { mutableStateOf(initialUserEmail) }
    var userPermissions by remember { mutableStateOf(initialPermissions) }
    var currentLanguage by remember { mutableStateOf(initialLanguage) }
    var languageCode by remember { mutableStateOf(initialLanguageCode) }

    // Refresh data when drawer opens (for Settings/permission changes)
    // Load in background to not block drawer animation
    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen) return@LaunchedEffect
        
        // Load in background IO coroutine that won't block drawer opening
        withContext(Dispatchers.IO) {
            try {
                val loadedUserName = preferenceHelper.getString(PrefConstant.USER_NAME, "User") ?: "User"
                val loadedUserEmail = preferenceHelper.getString(PrefConstant.USER_EMAIL, "") ?: ""
                val loadedPermissions = getUserPermissions(preferenceHelper)
                val loadedLanguage = preferenceHelper.getString(PrefConstant.CURRENT_LANGUAGE, "English") ?: "English"
                val loadedCode = LocalizationManager.getLanguageCode(loadedLanguage)

                withContext(Dispatchers.Main.immediate) {
                    userName = loadedUserName
                    userEmail = loadedUserEmail
                    userPermissions = loadedPermissions
                    currentLanguage = loadedLanguage
                    languageCode = loadedCode
                    android.util.Log.d("NavigationDrawer", "Drawer data refreshed")
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationDrawer", "Error refreshing drawer data: ${e.message}")
                // Keep existing values on error - don't reset to defaults
            }
        }
    }

    // Define drawer items with permission requirements - use derivedStateOf for performance
    val drawerItems by remember(languageCode) {
        derivedStateOf {
            try {
                android.util.Log.d("NavigationDrawer", "Building drawer items with languageCode: '$languageCode'")
                if (languageCode.isNotEmpty()) {
                    listOf(
                        DrawerItem(
                            id = "dashboard",
                            title = StringResources.getString(StringResources.DASHBOARD, languageCode),
                            icon = Icons.Default.Dashboard,
                            route = ScreenName.Dashboard
                        ),
                        DrawerItem(
                            id = "emptying_scheduling",
                            title = StringResources.getString(StringResources.EMPTYING_SCHEDULING, languageCode),
                            icon = Icons.Default.Schedule,
                            route = "emptying_scheduling",
                            requiredPermission = "Emptying Scheduling"
                        ),
                        DrawerItem(
                            id = "site_preparation",
                            title = StringResources.getString(StringResources.SITE_PREPARATION, languageCode),
                            icon = Icons.Default.Construction,
                            route = "site_preparation",
                            requiredPermission = "Site Preparation"
                        ),
                        DrawerItem(
                            id = "emptying_service",
                            title = StringResources.getString(StringResources.EMPTYING_SERVICE, languageCode),
                            icon = Icons.Default.CleaningServices,
                            route = "emptying_service",
                            requiredPermission = "Emptying Service"
                        ),
                        DrawerItem(
                            id = "todo_list",
                            title = StringResources.getString(StringResources.TODO_LIST, languageCode),
                            icon = Icons.AutoMirrored.Filled.List,
                            route = ScreenName.TodoList
                        ),
                        DrawerItem(
                            id = "desludging_vehicle",
                            title = StringResources.getString(StringResources.DESLUDGING_VEHICLES, languageCode),
                            icon = Icons.Default.LocalShipping,
                            route = ScreenName.DesludgingVehicle
                        )
                    )
                } else {
                    android.util.Log.w("NavigationDrawer", "Language code is empty, returning empty drawer items")
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationDrawer", "Error building drawer items: ${e.message}", e)
                emptyList()
            }
        }
    }

    // Filter items based on permissions - use derivedStateOf for better performance
    val visibleItems by remember(drawerItems, userPermissions) {
        derivedStateOf {
            drawerItems.filter { item ->
                item.requiredPermission == null || userPermissions[item.requiredPermission] == true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        scrimColor = Color.Black.copy(alpha = 0.1f), // Very subtle overlay to prevent black screen effect
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                android.util.Log.d("NavigationDrawer", "Rendering DrawerContent - userName: $userName, languageCode: $languageCode, visibleItems: ${visibleItems.size}")
                DrawerContent(
                    userName = userName ?: "User",
                    userEmail = userEmail ?: "",
                    currentLanguage = currentLanguage ?: "English",
                    languageCode = languageCode,
                    drawerItems = visibleItems,
                    onItemClick = { route ->
                        scope.launch {
                            try {
                                // CRITICAL: Wait for drawer to fully close before navigating
                                drawerState.close()
                                navController.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationDrawer", "Navigation error: ${e.message}")
                            }
                        }
                    },
                    onSettingsClick = {
                        scope.launch {
                            try {
                                // CRITICAL: Wait for drawer to fully close before navigating
                                drawerState.close()
                                navController.navigate(ScreenName.Settings) {
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationDrawer", "Settings navigation error: ${e.message}")
                            }
                        }
                    },
                    onLogoutClick = {
                        scope.launch {
                            try {
                                // CRITICAL: Wait for drawer to fully close before navigating
                                drawerState.close()
                                // Clear authentication data
                                withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    preferenceHelper.setBoolean(PrefConstant.IS_LOGIN, false)
                                    preferenceHelper.setString(PrefConstant.AUTH_TOKEN, "")
                                    preferenceHelper.setString(PrefConstant.USER_PERMISSIONS, "")
                                }
                                topLevelNavController.navigate(ScreenName.Login) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationDrawer", "Logout navigation error: ${e.message}")
                            }
                        }
                    }
                )
            }
        },
        content = {
            content {
                val currentTime = System.currentTimeMillis()
                
                // Simple debounce - prevent rapid clicks within 300ms
                if (currentTime - lastMenuClickTime < 300) {
                    return@content
                }
                
                lastMenuClickTime = currentTime
                
                scope.launch {
                    try {
                        if (drawerState.isClosed) {
                            drawerState.open()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NavigationDrawer", "Error opening drawer: ${e.message}")
                    }
                }
            }
        }
    )
}

@Composable
private fun DrawerContent(
    userName: String,
    userEmail: String,
    currentLanguage: String,
    languageCode: String,
    drawerItems: List<DrawerItem>,
    onItemClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // User Profile Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (userEmail.isNotEmpty()) {
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Navigation Items - Use Column instead of LazyColumn for better performance with small lists
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            drawerItems.forEach { item ->
                DrawerMenuItem(
                    item = item,
                    onClick = { onItemClick(item.route) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Bottom Section - Settings, Logout
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Settings
            DrawerMenuItem(
                item = DrawerItem(
                    id = "settings",
                    title = StringResources.getString(StringResources.SETTINGS, languageCode),
                    icon = Icons.Default.Settings,
                    route = ScreenName.Settings
                ),
                onClick = onSettingsClick
            )

            // Logout
            DrawerMenuItem(
                item = DrawerItem(
                    id = "logout",
                    title = StringResources.getString(StringResources.LOGOUT, languageCode),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    route = ""
                ),
                onClick = onLogoutClick,
                isDestructive = true
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    item: DrawerItem,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isDestructive) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "Khmer")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${StringResources.getString(StringResources.LANGUAGE, LocalizationManager.getLanguageCode(currentLanguage))}: $currentLanguage",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language) },
                    onClick = {
                        onLanguageChange(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Cached permissions to avoid repeated parsing
private var cachedPermissions: Map<String, Boolean>? = null
private var lastPermissionsString: String? = null

// Optimized helper function to parse permissions from SharedPreferences
private fun getUserPermissions(preferenceHelper: PreferenceHelper): Map<String, Boolean> {
    try {
        val permissionsJson = preferenceHelper.getString(PrefConstant.USER_PERMISSIONS, "") ?: ""

        // Return cached result if the permissions string hasn't changed
        if (permissionsJson == lastPermissionsString && cachedPermissions != null) {
            return cachedPermissions!!
        }

        if (permissionsJson.isEmpty()) {
            cachedPermissions = emptyMap()
            lastPermissionsString = permissionsJson
            return emptyMap()
        }

        // Parse permissions from Kotlin List toString format
        // Format: [UserPermission(viewMap=true, editBuildingSurvey=false, ...)]
        val permissions = mutableMapOf<String, Boolean>()

        try {
            val cleanStr = permissionsJson.trim()
            
            // Extract content between UserPermission( and )
            if (cleanStr.contains("UserPermission(")) {
                val start = cleanStr.indexOf("UserPermission(") + "UserPermission(".length
                val end = cleanStr.lastIndexOf(")")
                
                if (start < end) {
                    val content = cleanStr.substring(start, end)
                    
                    // Parse field=value pairs and map to permission names
                    content.split(",").forEach { pair ->
                        val parts = pair.trim().split("=", limit = 2)
                        if (parts.size == 2) {
                            val fieldName = parts[0].trim()
                            val value = parts[1].trim().toBooleanStrictOrNull() ?: false
                            
                            // Map field names to permission names as they appear in API
                            val permissionName = when (fieldName) {
                                "viewMap" -> "View Map"
                                "editBuildingSurvey" -> "Edit Building Survey"
                                "emptyingScheduling" -> "Emptying Scheduling"
                                "sitePreparation" -> "Site Preparation"
                                "emptying" -> "Emptying Service"
                                else -> null
                            }
                            
                            if (permissionName != null) {
                                permissions[permissionName] = value
                            }
                        }
                    }
                }
            }
            
            android.util.Log.d("NavigationDrawer", "Parsed ${permissions.size} permissions from stored format")
            
            cachedPermissions = permissions.toMap()
            lastPermissionsString = permissionsJson
            return permissions

        } catch (e: Exception) {
            android.util.Log.w("NavigationDrawer", "Permission parsing failed: ${e.message}")
            // Return cached permissions if available, otherwise empty map (safe default)
            val result = cachedPermissions ?: emptyMap()
            return result
        }

    } catch (e: Exception) {
        android.util.Log.e("NavigationDrawer", "getUserPermissions error: ${e.message}")
        // Return cached permissions if available, otherwise empty map
        return cachedPermissions ?: emptyMap()
    }
}