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
import kotlinx.coroutines.launch


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

    // Add debouncing and state tracking to prevent race conditions
    var lastMenuClickTime by remember { mutableLongStateOf(0L) }
    var isNavigationInProgress by remember { mutableStateOf(false) }

    // Track navigation state changes to prevent drawer operations during navigation
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            android.util.Log.d("NavigationDrawer", "Navigation to: ${destination.route}")
            isNavigationInProgress = true
            // Reset navigation flag after a short delay
            scope.launch {
                kotlinx.coroutines.delay(300) // Wait for navigation to settle
                isNavigationInProgress = false
                android.util.Log.d("NavigationDrawer", "Navigation settled")
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // Get user info and permissions - use LaunchedEffect to prevent blocking main thread
    var userName by remember { mutableStateOf("User") }
    var userEmail by remember { mutableStateOf("") }
    var userPermissions by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    var currentLanguage by remember { mutableStateOf("English") }
    var languageCode by remember { mutableStateOf("en") }

    // Load user data asynchronously with timeout protection
    LaunchedEffect(Unit) {
        try {
            // Use withTimeout to prevent hanging operations
            kotlinx.coroutines.withTimeout(2000) { // 2 second timeout
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    android.util.Log.d("NavigationDrawer", "Loading user data...")

                    // Load basic data quickly
                    val loadedUserName = preferenceHelper.getString(PrefConstant.USER_NAME, "User")
                    val loadedUserEmail = preferenceHelper.getString(PrefConstant.USER_EMAIL, "")
                    val loadedCurrentLanguage = preferenceHelper.getString(PrefConstant.CURRENT_LANGUAGE, "English")
                    val loadedLanguageCode = LocalizationManager.getLanguageCode(loadedCurrentLanguage ?: "English")

                    // Load permissions with optimization
                    val startTime = System.currentTimeMillis()
                    val loadedUserPermissions = getUserPermissions(preferenceHelper)
                    val permissionLoadTime = System.currentTimeMillis() - startTime
                    android.util.Log.d("NavigationDrawer", "Permissions loaded in ${permissionLoadTime}ms")

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                        userName = loadedUserName ?: "User"
                        userEmail = loadedUserEmail ?: ""
                        userPermissions = loadedUserPermissions
                        currentLanguage = loadedCurrentLanguage ?: "English"
                        languageCode = loadedLanguageCode
                        android.util.Log.d("NavigationDrawer", "User data loaded successfully in ${System.currentTimeMillis() - startTime}ms total")
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.w("NavigationDrawer", "User data loading timed out, using defaults")
            // Set fallback values if loading times out
            userName = "User"
            userEmail = ""
            userPermissions = mapOf(
                "View Map" to true,
                "Emtying Scheduling" to true,
                "Site Preparation" to true,
                "Emtying Service" to true,
                "Task Management" to true,
                "Edit Building Survey" to true,
                "Desludging Vehicle" to true
            )
            currentLanguage = "English"
            languageCode = "en"
        } catch (e: Exception) {
            android.util.Log.e("NavigationDrawer", "Error loading user data: ${e.message}")
            // Set fallback values if loading fails
            userName = "User"
            userEmail = ""
            userPermissions = mapOf(
                "View Map" to true,
                "Emtying Scheduling" to true,
                "Site Preparation" to true,
                "Emtying Service" to true,
                "Task Management" to true,
                "Edit Building Survey" to true,
                "Desludging Vehicle" to true
            )
            currentLanguage = "English"
            languageCode = "en"
        }
    }

    // Define drawer items with permission requirements - use derivedStateOf for performance
    val drawerItems by remember(languageCode) {
        derivedStateOf {
            if (languageCode.isNotEmpty()) {
                listOf(
                    DrawerItem(
                        id = "map",
                        title = StringResources.getString(StringResources.MAP, languageCode),
                        icon = Icons.Default.Map,
                        route = ScreenName.Map,
                        requiredPermission = "View Map"
                    ),
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
                        requiredPermission = "Emtying Scheduling"
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
                        requiredPermission = "Emptying"
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
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                DrawerContent(
                    userName = userName ?: "User",
                    userEmail = userEmail ?: "",
                    currentLanguage = currentLanguage ?: "English",
                    drawerItems = visibleItems,
                    onItemClick = { route ->
                        scope.launch(kotlinx.coroutines.Dispatchers.Main.immediate) {
                            try {
                                // Set navigation in progress flag
                                isNavigationInProgress = true

                                if (drawerState.isOpen) {
                                    drawerState.close()
                                    // Wait for drawer to close before navigating
                                    kotlinx.coroutines.delay(150)
                                }

                                navController.navigate(route) {
                                    // Clear back stack if navigating to main screens
                                    if (route in listOf(ScreenName.Dashboard, ScreenName.Map)) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } catch (e: Exception) {
                                // Handle navigation errors gracefully
                                android.util.Log.e("NavigationDrawer", "Navigation error: ${e.message}")
                                isNavigationInProgress = false
                            }
                        }
                    },
                    onSettingsClick = {
                        scope.launch(kotlinx.coroutines.Dispatchers.Main.immediate) {
                            try {
                                // Set navigation in progress flag
                                isNavigationInProgress = true

                                if (drawerState.isOpen) {
                                    drawerState.close()
                                    kotlinx.coroutines.delay(150)
                                }
                                navController.navigate(ScreenName.Settings) {
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationDrawer", "Settings navigation error: ${e.message}")
                                isNavigationInProgress = false
                            }
                        }
                    },
                    onLogoutClick = {
                        scope.launch(kotlinx.coroutines.Dispatchers.Main.immediate) {
                            try {
                                // Set navigation in progress flag
                                isNavigationInProgress = true

                                if (drawerState.isOpen) {
                                    drawerState.close()
                                    kotlinx.coroutines.delay(150)
                                }
                                // Clear authentication data immediately
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                                isNavigationInProgress = false
                            }
                        }
                    }
                )
            }
        },
        content = {
            content {
                val currentTime = System.currentTimeMillis()
                android.util.Log.d("NavigationDrawer", "Menu click received - drawer state: ${drawerState.currentValue}, navigationInProgress: $isNavigationInProgress")

                // Debounce clicks - prevent rapid clicks within 500ms
                if (currentTime - lastMenuClickTime < 500) {
                    android.util.Log.d("NavigationDrawer", "Menu click ignored - too fast (debounced)")
                    return@content
                }

                // Prevent drawer operations during navigation transitions
                if (isNavigationInProgress) {
                    android.util.Log.d("NavigationDrawer", "Menu click ignored - navigation in progress")
                    return@content
                }

                lastMenuClickTime = currentTime

                // Use Dispatchers.Main.immediate to avoid coroutine overhead on main thread
                scope.launch(kotlinx.coroutines.Dispatchers.Main.immediate) {
                    try {
                        // Double-check navigation state inside the coroutine
                        if (isNavigationInProgress) {
                            android.util.Log.d("NavigationDrawer", "Menu click ignored - navigation in progress (double-check)")
                            return@launch
                        }

                        if (drawerState.isClosed) {
                            android.util.Log.d("NavigationDrawer", "Opening drawer...")

                            // Add timeout for drawer opening to prevent hanging
                            kotlinx.coroutines.withTimeout(2000) { // Reduced timeout for faster response
                                drawerState.open()
                            }
                            android.util.Log.d("NavigationDrawer", "Drawer opened successfully")
                        } else {
                            android.util.Log.d("NavigationDrawer", "Drawer already open - state: ${drawerState.currentValue}")
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        android.util.Log.w("NavigationDrawer", "Drawer opening timed out - ignoring click")
                        // Don't force navigation on timeout, just ignore the click
                    } catch (e: Exception) {
                        android.util.Log.e("NavigationDrawer", "Error opening drawer: ${e.message}", e)
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
                    title = StringResources.getString(StringResources.SETTINGS, LocalizationManager.getLanguageCode(currentLanguage ?: "English")),
                    icon = Icons.Default.Settings,
                    route = ScreenName.Settings
                ),
                onClick = onSettingsClick
            )

            // Logout
            DrawerMenuItem(
                item = DrawerItem(
                    id = "logout",
                    title = StringResources.getString(StringResources.LOGOUT, LocalizationManager.getLanguageCode(currentLanguage ?: "English")),
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

        // Simplified and faster parsing - avoid multiple string operations
        val permissions = mutableMapOf<String, Boolean>()

        // Basic permission set for SMIS app - fallback if parsing fails
        val defaultPermissions = mapOf(
            "View Map" to true,
            "Emtying Scheduling" to true,
            "Site Preparation" to true,
            "Emtying Service" to true,
            "Task Management" to true,
            "Edit Building Survey" to true,
            "Desludging Vehicle" to true
        )

        // Try to parse, but if it takes too long or fails, use defaults
        try {
            if (permissionsJson.length < 1000) {
                val cleanJson = permissionsJson.trim()
                if (cleanJson.startsWith("{") || cleanJson.startsWith("[")) {
                    permissions.putAll(defaultPermissions)
                } else {
                    // Try simple key=value parsing
                    cleanJson.split(",").take(20).forEach { pair ->
                        val keyValue = pair.split(":", limit = 2)
                        if (keyValue.size == 2) {
                            val key = keyValue[0].trim().replace("\"", "")
                            val value = keyValue[1].trim().replace("\"", "").toBooleanStrictOrNull() ?: true
                            permissions[key] = value
                        }
                    }
                }
            } else {
                // Too large, use defaults
                permissions.putAll(defaultPermissions)
            }
        } catch (e: Exception) {
            android.util.Log.w("NavigationDrawer", "Permission parsing failed, using defaults: ${e.message}")
            permissions.putAll(defaultPermissions)
        }

        cachedPermissions = permissions.toMap()
        lastPermissionsString = permissionsJson

        return permissions

    } catch (e: Exception) {
        android.util.Log.e("NavigationDrawer", "getUserPermissions error: ${e.message}")
        return mapOf(
            "View Map" to true,
            "Emtying Scheduling" to true,
            "Site Preparation" to true,
            "Emtying Service" to true,
            "Task Management" to true,
            "Edit Building Survey" to true,
            "Desludging Vehicle" to true
        )
    }
}