package com.innovative.smis.ui.components

import com.innovative.smis.R

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.innovative.smis.util.constants.NavigationIcons
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.helper.PreferenceHelper

/**
 * Bottom navigation bar with 5 main screens.
 * Only shows items that user has permission to access.
 */
@Composable
fun AppBottomNavigation(
    navController: NavController,
    preferenceHelper: PreferenceHelper
) {
    val context = LocalContext.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    
    // Load string resources FIRST (must be called directly in Composable)
    val dashboardLabel = stringResource(R.string.nav_dashboard)
    val emptyingSchedulingLabel = stringResource(R.string.nav_emptying_scheduling)
    val sitePreparationLabel = stringResource(R.string.nav_site_preparation)
    val emptyingServiceLabel = stringResource(R.string.nav_emptying_service)
    val desludgingVehicleLabel = stringResource(R.string.nav_desludging_vehicles)
    
    // Get user permissions
    val userPermissions = remember(preferenceHelper) {
        val permissions = mutableMapOf<String, Boolean>()
        try {
            val permissionsJson = preferenceHelper.getString(PrefConstant.USER_PERMISSIONS, "{}") ?: "{}"
            val permissionFields = listOf("emptyingScheduling", "sitePreparation", "emptying")
            
            permissionFields.forEach { fieldName ->
                val permissionName = when (fieldName) {
                    "emptyingScheduling" -> "Emptying Scheduling"
                    "sitePreparation" -> "Site Preparation"
                    "emptying" -> "Emptying Service"
                    else -> null
                }
                
                if (permissionName != null) {
                    permissions[permissionName] = permissionsJson.contains("\"$fieldName\":true")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppBottomNavigation", "Error loading permissions: ${e.message}")
        }
        permissions
    }
    
    // Define bottom nav items (using pre-loaded string resources)
    val bottomNavItems = remember(
        dashboardLabel,
        emptyingSchedulingLabel,
        sitePreparationLabel,
        emptyingServiceLabel,
        desludgingVehicleLabel
    ) {
        listOf(
            BottomNavItem(
                route = ScreenName.Dashboard,
                icon = NavigationIcons.Dashboard,
                label = dashboardLabel
            ),
            BottomNavItem(
                route = "emptying_scheduling",
                icon = NavigationIcons.EmptyingScheduling,
                label = emptyingSchedulingLabel,
                requiredPermission = "Emptying Scheduling"
            ),
            BottomNavItem(
                route = "site_preparation",
                icon = NavigationIcons.SitePreparation,
                label = sitePreparationLabel,
                requiredPermission = "Site Preparation"
            ),
            BottomNavItem(
                route = "emptying_service",
                icon = NavigationIcons.EmptyingService,
                label = emptyingServiceLabel,
                requiredPermission = "Emptying Service"
            ),
            BottomNavItem(
                route = ScreenName.DesludgingVehicle,
                icon = NavigationIcons.DesludgingVehicle,
                label = desludgingVehicleLabel
            )
        )
    }
    
    // Filter items based on permissions with null safety
    val visibleItems = remember(bottomNavItems, userPermissions) {
        try {
            bottomNavItems.filter { item ->
                item.requiredPermission == null || userPermissions[item.requiredPermission] == true
            }
        } catch (e: Exception) {
            android.util.Log.e("AppBottomNavigation", "Error filtering items: ${e.message}")
            // Return at least Dashboard if filtering fails
            listOf(
                BottomNavItem(
                    route = ScreenName.Dashboard,
                    icon = NavigationIcons.Dashboard,
                    label = dashboardLabel
                )
            )
        }
    }
    
    // Only render if we have items
    if (visibleItems.isNotEmpty()) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            visibleItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to start destination to avoid building large back stack
                            popUpTo(ScreenName.Dashboard) {
                                saveState = true
                            }
                            // Avoid multiple copies of same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                alwaysShowLabel = false
            )
            }
        }
    }
}

/**
 * Data class representing a bottom navigation item
 */
private data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val requiredPermission: String? = null
)
