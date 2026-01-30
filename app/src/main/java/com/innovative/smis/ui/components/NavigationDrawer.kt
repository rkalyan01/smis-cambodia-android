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
import com.innovative.smis.R
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import com.innovative.smis.util.navigation.navigateSafe
import com.innovative.smis.util.ui.clickableDebounced
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build

/**
 * ⚡ MIUI FIX: Close drawer instantly on MIUI/HyperOS to prevent black screen
 * 
 * Black screen on MIUI is caused by ModalNavigationDrawer's closing animation
 * conflicting with MIUI's GPU compositor. By using snapTo() instead of close(),
 * we skip the animation and prevent the black frame issue.
 * 
 * Reference: Compose Material3 issues #345213882, #341897716
 */
private suspend fun DrawerState.closeSafely() {
    if (!isClosed) {
        // Check if device is Xiaomi/Redmi/POCO (MIUI/HyperOS)
        val isMIUI = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                     Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
                     Build.MANUFACTURER.equals("POCO", ignoreCase = true)
        
        if (isMIUI) {
            // Instant close (no animation) on MIUI to prevent black screen
            android.util.Log.d("NavigationDrawer", "⚡ MIUI detected - using instant drawer close")
            snapTo(DrawerValue.Closed)
        } else {
            // Animated close on other devices
            close()
        }
    }
}

data class DrawerItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val route: String,
    val requiredPermission: String? = null,
    val requiredRole: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawer(
    navController: NavController,
    topLevelNavController: NavController,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true, // 🔧 FIX: Gate gestures on destination lifecycle (isDestinationStable)
    onMenuClick: () -> Unit, // ✅ Receive the stable lambda as a parameter
    content: @Composable () -> Unit // ✅ Changed signature - no longer provides onMenuClick
) {
    val context = LocalContext.current
    val preferenceHelper = PreferenceHelper(context)
    val scope = rememberCoroutineScope()

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
            preferenceHelper.getUserPermissionsMap()
        } catch (e: Exception) {
            android.util.Log.e("NavigationDrawer", "Error loading permissions: ${e.message}", e)
            emptyMap()
        }
    }
    val initialUserRole = remember { 
        try {
            preferenceHelper.getUserRoles()
        } catch (e: Exception) {
            android.util.Log.e("NavigationDrawer", "Error loading user role: ${e.message}", e)
            emptyList()
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
    var userRole by remember { mutableStateOf(initialUserRole) }
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
                val loadedPermissions = preferenceHelper.getUserPermissionsMap()
                val loadedRole = preferenceHelper.getUserRoles()
                val loadedLanguage = preferenceHelper.getString(PrefConstant.CURRENT_LANGUAGE, "English") ?: "English"
                val loadedCode = LocalizationManager.getLanguageCode(loadedLanguage)

                withContext(Dispatchers.Main.immediate) {
                    userName = loadedUserName
                    userEmail = loadedUserEmail
                    userPermissions = loadedPermissions
                    userRole = loadedRole
                    currentLanguage = loadedLanguage
                    languageCode = loadedCode
                    android.util.Log.d("NavigationDrawer", "Drawer data refreshed (role: $loadedRole)")
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationDrawer", "Error refreshing drawer data: ${e.message}")
                // Keep existing values on error - don't reset to defaults
            }
        }
    }

    // Define drawer items with permission requirements
    // Use Compose stringResource() to get properly localized strings from XML
    val drawerItems = listOf(
        DrawerItem(
            id = "dashboard",
            title = stringResource(com.innovative.smis.R.string.nav_dashboard),
            icon = Icons.Default.Dashboard,
            route = ScreenName.Dashboard
        ),
        DrawerItem(
            id = "emptying_scheduling",
            title = stringResource(com.innovative.smis.R.string.nav_emptying_scheduling),
            icon = Icons.Default.Schedule,
            route = "emptying_scheduling"
        ),
        DrawerItem(
            id = "site_preparation",
            title = stringResource(com.innovative.smis.R.string.nav_site_preparation),
            icon = Icons.Default.Construction,
            route = "site_preparation"
        ),
        DrawerItem(
            id = "emptying_service",
            title = stringResource(com.innovative.smis.R.string.nav_emptying_service),
            icon = Icons.Default.Build,
            route = "emptying_service"
        ),
        DrawerItem(
            id = "additional_trips",
            title = stringResource(com.innovative.smis.R.string.nav_additional_trips),
            icon = Icons.Default.AddCircle,
            route = "additional_repairing"
        ),
        DrawerItem(
            id = "desludging_vehicle",
            title = stringResource(com.innovative.smis.R.string.nav_desludging_vehicles),
            icon = Icons.Default.LocalShipping,
            route = ScreenName.DesludgingVehicle,
            requiredRole = "ETO Admin"
        ),
        DrawerItem(
            id = "eto_license_status",
            title = stringResource(R.string.nav_eto_license_status),
            icon = Icons.Default.CardMembership,
            route = ScreenName.EtoLicenseStatus
            // requiredRole = "ETO Admin" // Uncomment if you want to restrict this to Admins only
        ),
        DrawerItem(
            id = "building_map",
            title = "Building Map", // TODO: Add string resource
            icon = Icons.Default.Map,
            route = ScreenName.BuildingMap
            // Visible to everyone (filtered below for Enumerator exclusivity)
        )
    )

    // Filter items based on permissions and roles - use derivedStateOf for better performance
    val visibleItems by remember(drawerItems, userPermissions, userRole) {
        derivedStateOf {
            // Check if user is an Enumerator (case-insensitive check)
            val isEnumerator = userRole.any { it.equals("Enumerator", ignoreCase = true) }
            
            drawerItems.filter { item ->
                // Building Map is ONLY visible to Enumerator
                if (item.route == ScreenName.BuildingMap) {
                    isEnumerator
                } else {
                    // IF user is Enumerator, they should NOT see anything else
                    if (isEnumerator) {
                        false
                    } else {
                        // Normal checks for other items for non-Enumerators
                        val hasPermission = item.requiredPermission == null || userPermissions[item.requiredPermission] == true
                        val hasRole = item.requiredRole == null || userRole.contains(item.requiredRole)
                        hasPermission && hasRole
                    }
                }
            }
        }
    }

    // ✅ Drawer state monitoring removed - gestures now controlled by parent's isDestinationStable

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled, // ✅ Controlled by parent (isDestinationStable)
        scrimColor = Color.Black.copy(alpha = 0.1f), // Very subtle overlay to prevent black screen effect
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Always render drawer content - safe navigation handles lifecycle checks
                android.util.Log.d("NavigationDrawer", "Rendering DrawerContent - userName: $userName, languageCode: $languageCode, visibleItems: ${visibleItems.size}")
                DrawerContent(
                    userName = userName ?: "User",
                    userEmail = userEmail ?: "",
                    currentLanguage = currentLanguage ?: "English",
                    drawerItems = visibleItems,
                    onItemClick = { route ->
                        android.util.Log.d("NavigationDrawer", "📍 Drawer item clicked: $route, currentRoute: ${navController.currentDestination?.route}")
                        scope.launch {
                            try {
                                // ⚡ MIUI FIX: Close drawer safely (instant on MIUI, animated on others)
                                drawerState.closeSafely()
                                // Wait for close to complete
                                snapshotFlow { drawerState.isClosed }.first { it }
                                
                                android.util.Log.d("NavigationDrawer", "🧭 Navigating to: $route")
                                // Use safe navigation to prevent race conditions
                                navController.navigateSafe(route)
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationDrawer", "Navigation error: ${e.message}")
                            }
                        }
                    },
                    onSettingsClick = {
                        scope.launch {
                            try {
                                // ⚡ MIUI FIX: Close drawer safely (instant on MIUI, animated on others)
                                drawerState.closeSafely()
                                // Wait for close to complete
                                snapshotFlow { drawerState.isClosed }.first { it }
                                
                                // Use safe navigation to prevent race conditions
                                navController.navigateSafe(ScreenName.Settings)
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationDrawer", "Settings navigation error: ${e.message}")
                            }
                        }
                    },
                    onLogoutClick = {
                        scope.launch {
                            try {
                                // ⚡ MIUI FIX: Close drawer safely (instant on MIUI, animated on others)
                                drawerState.closeSafely()
                                // Wait for close to complete
                                snapshotFlow { drawerState.isClosed }.first { it }
                                
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
            // ✅ Just render the content (the NavHost) - onMenuClick is now passed as a parameter
            content()
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
                    title = stringResource(com.innovative.smis.R.string.nav_settings),
                    icon = Icons.Default.Settings,
                    route = ScreenName.Settings
                ),
                onClick = onSettingsClick
            )

            // Logout
            DrawerMenuItem(
                item = DrawerItem(
                    id = "logout",
                    title = stringResource(com.innovative.smis.R.string.nav_logout),
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
            .clickableDebounced(debounceTime = 500L) { onClick() },
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


