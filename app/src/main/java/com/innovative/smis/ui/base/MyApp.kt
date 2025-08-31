package com.innovative.smis.ui.base

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.innovative.smis.ui.components.AppNavigationDrawer
import com.innovative.smis.ui.features.buildingsurvey.BuildingSurveyScreen
import com.innovative.smis.ui.features.buildingsurvey.ComprehensiveSurveyScreen
import com.innovative.smis.ui.features.containment.ContainmentFormScreen
import com.innovative.smis.ui.features.dashboard.DashboardScreen
import com.innovative.smis.ui.features.desludgingvehicle.DesludgingVehicleScreen
import com.innovative.smis.ui.features.emptyingscheduling.EmptyingSchedulingFormScreen
import com.innovative.smis.ui.features.emptyingscheduling.EmptyingSchedulingScreen
import com.innovative.smis.ui.features.emptyingservice.EmptyingServiceFormScreen
import com.innovative.smis.ui.features.emptyingservice.EmptyingServiceScreen
import com.innovative.smis.ui.features.login.LoginScreen
import com.innovative.smis.ui.features.map.MapScreen
import com.innovative.smis.ui.features.permissions.PermissionRequestScreen
import com.innovative.smis.ui.features.settings.SettingsScreen
import com.innovative.smis.ui.features.sitepreparation.SitePreparationFormScreen
import com.innovative.smis.ui.features.sitepreparation.SitePreparationScreen
import com.innovative.smis.ui.features.taskmanagement.TaskManagementScreen
import com.innovative.smis.ui.features.todolist.TodoListScreen
import com.innovative.smis.ui.theme.ThemeProvider
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.navArgument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    val context = LocalContext.current
    val preferenceHelper = PreferenceHelper(context)
    val isLoggedIn = preferenceHelper.getBoolean(PrefConstant.IS_LOGIN, false)
    val permissionsRequested = preferenceHelper.getBoolean(PrefConstant.PERMISSIONS_REQUESTED, false)
    val userPermissions = getUserPermissions(preferenceHelper)

    // Permission-based navigation: Show permissions first, then login, then app
    val hasMapPermission = userPermissions["View Map"] == true
    val hasOtherPermissions = userPermissions.values.any { it == true && userPermissions["View Map"] != it }
    val startDestination = when {
        !permissionsRequested -> "permissions"
        !isLoggedIn -> ScreenName.Login
        hasMapPermission && hasOtherPermissions -> "main_app"
        hasMapPermission -> "main_app"
        else -> "main_app"
    }

    ThemeProvider {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = startDestination) {
            // Permission Request Screen
            composable("permissions") {
                PermissionRequestScreen(
                    onPermissionsGranted = {
                        navController.navigate(
                            if (isLoggedIn) {
                                "main_app"
                            } else {
                                ScreenName.Login
                            }
                        ) {
                            popUpTo("permissions") { inclusive = true }
                        }
                    }
                )
            }

            composable(ScreenName.Login) {
                LoginScreen(navController = navController)
            }

            // This is the main destination for the logged-in user.
            // It contains the Drawer and the rest of the app's screens.
            composable("main_app") {
                MainAppScreen(topLevelNavController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScreen(topLevelNavController: NavController) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // The AppNavigationDrawer is now at the top level. It will only be created ONCE.
    AppNavigationDrawer(
        navController = navController,
        topLevelNavController = topLevelNavController,
        drawerState = drawerState,
        gesturesEnabled = true
    ) { onMenuClick ->
        // The NavHost is now INSIDE the drawer's content.
        NavHost(navController = navController, startDestination = ScreenName.Dashboard) {

            // Define all your screens here. They will be displayed within the drawer's content area.
            composable(ScreenName.Dashboard) {
                DashboardScreen(navController = navController, onMenuClick = onMenuClick)
            }
            composable(ScreenName.Map) {
                MapScreen(navController = navController, onMenuClick = onMenuClick)
            }
            composable("emptying_scheduling") {
                EmptyingSchedulingScreen(navController = navController)
            }
            composable("site_preparation") {
                SitePreparationScreen(navController = navController)
            }
            composable("emptying_service") {
                EmptyingServiceScreen(navController = navController)
            }
            composable(ScreenName.TodoList) {
                TodoListScreen(navController = navController)
            }
            composable(ScreenName.Settings) {
                SettingsScreen(navController = navController)
            }
            composable("task_management") {
                TaskManagementScreen(navController = navController)
            }
            composable(ScreenName.DesludgingVehicle) {
                DesludgingVehicleScreen(navController = navController)
            }

            // Form Screens (Now they don't need the drawer wrapped around them)
            composable(
                "emptying_scheduling_form/{applicationId}",
                arguments = listOf(navArgument("applicationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val applicationId = backStackEntry.arguments?.getString("applicationId")?.toIntOrNull()
                EmptyingSchedulingFormScreen(navController = navController, applicationId = applicationId)
            }

            composable(
                "site_preparation_form/{applicationId}",
                arguments = listOf(navArgument("applicationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val applicationId = backStackEntry.arguments?.getString("applicationId")?.toIntOrNull() ?: 0
                SitePreparationFormScreen(
                    applicationId = applicationId,
                    navController = navController,
                    onNavigateToContainment = { id -> navController.navigate("containment_form/$id") }
                )
            }

            composable(
                "emptying_service_form/{applicationId}",
                arguments = listOf(navArgument("applicationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val applicationId = backStackEntry.arguments?.getString("applicationId")?.toIntOrNull() ?: 0
                EmptyingServiceFormScreen(navController = navController, applicationId = applicationId)
            }

            composable(
                "containment_form/{applicationId}",
                arguments = listOf(navArgument("applicationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val applicationId = backStackEntry.arguments?.getString("applicationId") ?: "0"
                ContainmentFormScreen(
                    navController = navController,
                    applicationId = applicationId
                )
            }
            composable("building_survey_new") {
                BuildingSurveyScreen(navController = navController, bin = null)
            }

            composable(
                "building_survey_comprehensive/{bin}",
                arguments = listOf(navArgument("bin") { type = NavType.StringType })
            ) { backStackEntry ->
                val bin = backStackEntry.arguments?.getString("bin")
                ComprehensiveSurveyScreen(
                    navController = navController,
                    bin = bin
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
    // Basic permission set for SMIS app
    val defaultPermissions = mapOf(
        "View Map" to true,
        "Emtying Scheduling" to true,
        "Site Preparation" to true,
        "Emtying Service" to true,
        "Task Management" to true,
        "Edit Building Survey" to true,
        "Desludging Vehicle" to true
    )
    return defaultPermissions
}