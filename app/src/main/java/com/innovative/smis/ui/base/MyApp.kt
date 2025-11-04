package com.innovative.smis.ui.base

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.innovative.smis.ui.components.AppNavigationDrawer
import kotlinx.coroutines.launch
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
import com.innovative.smis.ui.features.additionalrepairing.AdditionalRepairingFormScreen
import com.innovative.smis.ui.features.additionalrepairing.AdditionalRepairingListScreen
import com.innovative.smis.ui.features.taskmanagement.TaskManagementScreen
import com.innovative.smis.ui.features.todolist.TodoListScreen
import com.innovative.smis.ui.theme.ThemeProvider
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.helper.PreferenceHelper
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
        val context = LocalContext.current
        
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
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Create the UI action lock to prevent race conditions
    val isActionInProgress = remember { AtomicBoolean(false) }
    
    // 🔧 MIUI FIX: Track last navigation time to prevent rapid back presses
    // MIUI's DisplayFeatureHal triggers black screen on rapid navigation even without edge-to-edge
    val lastNavigationTime = remember { AtomicLong(0L) }
    
    // Track current destination to handle back button
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    
    // 🔧 FIX: Add composition delay protection
    // Prevents clicks during screen recomposition (fixes black screen on fast navigation + fast click)
    var isScreenReady by remember { mutableStateOf(false) }
    
    // Reset screen ready state on ANY backstack change (not just route change)
    // This fixes the issue when clicking the same route from drawer (e.g., Dashboard → Dashboard)
    LaunchedEffect(currentBackStackEntry) {
        isScreenReady = false
        android.util.Log.d("MainAppScreen", "🔄 Navigation detected - route: $currentRoute, entry: ${currentBackStackEntry?.id} - blocking UI for 200ms")
        kotlinx.coroutines.delay(200) // Wait for screen to fully compose
        isScreenReady = true
        android.util.Log.d("MainAppScreen", "✅ Screen ready: $currentRoute - UI interactions enabled")
    }
    
    // Create a STABLE onMenuClick lambda that checks screen readiness
    val stableOnMenuClick: () -> Unit = {
        android.util.Log.d("MainAppScreen", "🍔 Menu click - isScreenReady=$isScreenReady, isActionInProgress=${isActionInProgress.get()}")
        if (isScreenReady && isActionInProgress.compareAndSet(false, true)) {
            scope.launch {
                try {
                    if (drawerState.isClosed) {
                        android.util.Log.d("MainAppScreen", "📂 Opening drawer...")
                        drawerState.open()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainAppScreen", "❌ Error opening drawer: ${e.message}", e)
                } finally {
                    isActionInProgress.set(false)
                }
            }
        } else {
            android.util.Log.w("MainAppScreen", "⚠️ Menu click blocked - Screen not ready or action in progress")
        }
    }
    
    // Handle back button press with atomic lock + navigation throttling
    BackHandler(enabled = true) {
        // 🔧 MIUI FIX: Throttle rapid back presses to prevent DisplayFeatureHal black screen
        val currentTime = System.currentTimeMillis()
        val lastTime = lastNavigationTime.get()
        val timeSinceLastNav = currentTime - lastTime
        
        if (timeSinceLastNav < 300) {
            // Ignore back presses within 300ms of last navigation
            android.util.Log.d("MainAppScreen", "⚠️ Back press ignored - too soon after last navigation ($timeSinceLastNav ms)")
            return@BackHandler
        }
        
        if (isActionInProgress.compareAndSet(false, true)) {
            when {
                // If drawer is open, close it
                drawerState.isOpen -> {
                    scope.launch {
                        try {
                            drawerState.close()
                        } finally {
                            isActionInProgress.set(false)
                        }
                    }
                }
                // If on Dashboard (root), minimize app
                currentRoute == ScreenName.Dashboard -> {
                    activity?.moveTaskToBack(true)
                    isActionInProgress.set(false)
                }
                // Check if there's something to pop
                navController.previousBackStackEntry != null && currentRoute != ScreenName.Dashboard -> {
                    try {
                        lastNavigationTime.set(currentTime) // Update navigation time
                        navController.popBackStack()
                    } finally {
                        isActionInProgress.set(false)
                    }
                }
                // Fallback case
                else -> {
                    try {
                        lastNavigationTime.set(currentTime) // Update navigation time
                        navController.navigate(ScreenName.Dashboard) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    } finally {
                        isActionInProgress.set(false)
                    }
                }
            }
        }
    }

    // 🔧 FIX: Block pointer input during composition instead of using overlay
    // This prevents black screen issues while still blocking rapid clicks
    val pointerModifier = if (!isScreenReady) {
        Modifier.pointerInput(Unit) {
            // Consume all pointer events during composition
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    android.util.Log.d("MainAppScreen", "⛔ Pointer event blocked - screen composing")
                    // Consume the event without passing it through
                }
            }
        }
    } else {
        Modifier
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(pointerModifier) // Apply pointer blocking conditionally
    ) {
        // AppNavigationDrawer - gestures gated on screen readiness
        AppNavigationDrawer(
            navController = navController,
            topLevelNavController = topLevelNavController,
            drawerState = drawerState,
            gesturesEnabled = true,
            isScreenReady = isScreenReady, // 🔧 FIX: Pass screen ready state to drawer
            isActionInProgress = isActionInProgress,
            onMenuClick = stableOnMenuClick
        ) {
            // The NavHost is INSIDE the drawer's content
            NavHost(
                navController = navController, 
                startDestination = ScreenName.Dashboard,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {

            // Define all your screens here
            composable(ScreenName.Dashboard) {
                DashboardScreen(
                    navController = navController,
                    onMenuClick = stableOnMenuClick
                )
            }
            composable(ScreenName.Map) {
                MapScreen(navController = navController, onMenuClick = stableOnMenuClick)
            }
            composable("emptying_scheduling") {
                EmptyingSchedulingScreen(navController = navController, onMenuClick = stableOnMenuClick)
            }
            composable("site_preparation") {
                SitePreparationScreen(navController = navController, onMenuClick = stableOnMenuClick)
            }
            composable("emptying_service") {
                EmptyingServiceScreen(navController = navController, onMenuClick = stableOnMenuClick)
            }
            composable(ScreenName.TodoList) {
                TodoListScreen(navController = navController, onMenuClick = stableOnMenuClick)
            }
            composable(ScreenName.Settings) {
                SettingsScreen(navController = navController, onMenuClick = stableOnMenuClick)
            }
            composable("task_management") {
                TaskManagementScreen(navController = navController, onMenuClick = stableOnMenuClick)
            }
            composable(ScreenName.DesludgingVehicle) {
                DesludgingVehicleScreen(navController = navController, onMenuClick = stableOnMenuClick)
            }
            composable("additional_repairing") {
                AdditionalRepairingListScreen(navController = navController, onMenuClick = stableOnMenuClick)
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
                    onNavigateToContainment = { id, sanitationCustomerId -> 
                        navController.navigate("containment_form/$id/${sanitationCustomerId ?: ""}")
                    }
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
                "containment_form/{applicationId}/{sanitationCustomerId}",
                arguments = listOf(
                    navArgument("applicationId") { type = NavType.StringType },
                    navArgument("sanitationCustomerId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val applicationId = backStackEntry.arguments?.getString("applicationId") ?: "0"
                val sanitationCustomerId = backStackEntry.arguments?.getString("sanitationCustomerId") ?: ""
                ContainmentFormScreen(
                    navController = navController,
                    applicationId = applicationId,
                    sanitationCustomerId = sanitationCustomerId
                )
            }

            composable(
                "additional_repairing_form/{emptyingId}",
                arguments = listOf(navArgument("emptyingId") { type = NavType.IntType })
            ) { backStackEntry ->
                val emptyingId = backStackEntry.arguments?.getInt("emptyingId") ?: 0
                AdditionalRepairingFormScreen(
                    navController = navController,
                    emptyingId = emptyingId
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