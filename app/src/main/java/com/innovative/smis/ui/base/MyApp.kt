package com.innovative.smis.ui.base

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import com.innovative.smis.ui.components.AppNavigationDrawer
import kotlinx.coroutines.launch
import com.innovative.smis.ui.features.map.BuildingMapScreen
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
    val preferenceHelper = remember { PreferenceHelper(context) }
    
    // Load states synchronously on first composition to avoid race conditions
    val isLanguageSelected = remember { preferenceHelper.getBoolean(PrefConstant.IS_LANGUAGE_SELECTED, false) }
    val isLoggedIn = remember { preferenceHelper.getBoolean(PrefConstant.IS_LOGIN, false) }
    val permissionsRequested = remember { preferenceHelper.getBoolean(PrefConstant.PERMISSIONS_REQUESTED, false) }
    

    

    // Calculate start destination based on loaded state
    // Calculate start destination based on loaded state
    val startDestination = remember {
        when {
            // !isLanguageSelected -> "language_selection" // Skipped for new flow
            // !permissionsRequested -> "permissions" // Skipped for new flow
            !isLoggedIn -> ScreenName.Login
            else -> "main_app"
        }
    }
    
    // Store isEnumerator in a CompositionLocal or pass it down? 
    // For now, we'll handle the start destination of "main_app" inside MainAppScreen or pass a parameter.
    // Actually, MainAppScreen handles the internal navigation. We might need to pass the start route to it.

    ThemeProvider {
        val navController = rememberNavController()
        val context = LocalContext.current
        
        NavHost(navController = navController, startDestination = startDestination) {
            composable(ScreenName.Login) {
                LoginScreen(navController = navController)
            }

            // This is the main destination for the logged-in user.
            // It contains the Drawer and the rest of the app's screens.
            composable("main_app") {
                // Check for Enumerator role dynamically on navigation
                val userRoles = preferenceHelper.getUserRoles()
                val isEnumerator = userRoles.any { it.equals("Enumerator", ignoreCase = true) }
                
                 // Pass start route based on role
                val appStartRoute = if (isEnumerator) ScreenName.BuildingMap else ScreenName.Dashboard
                MainAppScreen(topLevelNavController = navController, startRoute = appStartRoute)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScreen(topLevelNavController: NavController, startRoute: String = ScreenName.Dashboard) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    
    // 🔧 FIX: Use Mutex instead of AtomicBoolean for lifecycle-safe locking
    val drawerMutex = remember { Mutex() }
    val backMutex = remember { Mutex() }
    
    // Track current destination to handle back button
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    
    // 🔧 FIX: Use LifecycleEventObserver to properly observe destination lifecycle changes
    // derivedStateOf doesn't observe lifecycle - it reads once and never updates!
    var isDestinationStable by remember { mutableStateOf(false) }
    
    // Observe lifecycle events and update stability state
    DisposableEffect(currentBackStackEntry) {
        val entry = currentBackStackEntry
        
        if (entry == null) {
            isDestinationStable = false
            android.util.Log.w("MainAppScreen", "📍 No destination entry - marking unstable")
            return@DisposableEffect onDispose { }
        }
        
        val observer = LifecycleEventObserver { _, event ->
            val wasStable = isDestinationStable
            // Consider STARTED and above as stable (MIUI toggles between STARTED/RESUMED frequently)
            isDestinationStable = entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            
            if (wasStable != isDestinationStable) {
                android.util.Log.d("MainAppScreen", "📍 Lifecycle event: $event, state: ${entry.lifecycle.currentState}, stable: $isDestinationStable, route: $currentRoute")
            }
        }
        
        entry.lifecycle.addObserver(observer)
        
        // Set initial state
        isDestinationStable = entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        android.util.Log.d("MainAppScreen", "📍 Initial state: ${entry.lifecycle.currentState}, stable: $isDestinationStable, route: $currentRoute")
        
        onDispose {
            entry.lifecycle.removeObserver(observer)
            android.util.Log.d("MainAppScreen", "📍 Removed lifecycle observer for route: $currentRoute")
        }
    }
    
    // 🔧 FIX: Automatically close drawer when destination becomes unstable
    LaunchedEffect(isDestinationStable) {
        if (!isDestinationStable && drawerState.isOpen) {
            android.util.Log.w("MainAppScreen", "⚠️ Destination became unstable - force closing drawer")
            drawerState.close()
        }
    }
    
    // Create a STABLE onMenuClick lambda that uses lifecycle-safe locking
    val stableOnMenuClick: () -> Unit = {
        android.util.Log.d("MainAppScreen", "🍔 Menu click - isDestinationStable=$isDestinationStable")
        
        // Only execute if destination is stable (prevents black/white screen)
        if (isDestinationStable) {
            scope.launch {
                // Use mutex to prevent concurrent drawer operations
                if (drawerMutex.tryLock()) {
                    try {
                        if (drawerState.isClosed) {
                            android.util.Log.d("MainAppScreen", "📂 Opening drawer...")
                            drawerState.open()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainAppScreen", "❌ Error opening drawer: ${e.message}", e)
                    } finally {
                        drawerMutex.unlock()
                    }
                } else {
                    android.util.Log.w("MainAppScreen", "⚠️ Menu click blocked - Drawer action already in progress")
                }
            }
        } else {
            android.util.Log.w("MainAppScreen", "⚠️ Menu click blocked - Destination not stable/resumed")
        }
    }
    
    // Handle back button press with lifecycle-safe locking + MIUI protection
    BackHandler(enabled = true) {
        // 🔧 MIUI FIX: Enhanced protection using MiuiInputGuard (500ms throttle)
        if (!com.innovative.smis.util.helper.MiuiInputGuard.shouldAllowBack()) {
            android.util.Log.d("MainAppScreen", "⚠️ Back button blocked by MiuiInputGuard throttle")
            return@BackHandler
        }
        
        // Early return if destination is not stable (prevents issues during navigation)
        if (!isDestinationStable && currentRoute != ScreenName.Dashboard) {
            android.util.Log.w("MainAppScreen", "⚠️ Back button blocked - Destination not stable during transition")
            return@BackHandler
        }
        
        scope.launch {
            // Use mutex to prevent concurrent back operations
            if (backMutex.tryLock()) {
                try {
                    when {
                        // If drawer is open, close it
                        drawerState.isOpen -> {
                            android.util.Log.d("MainAppScreen", "🔙 Closing drawer")
                            drawerState.close()
                        }
                        // If on Dashboard (root), minimize app
                        currentRoute == ScreenName.Dashboard -> {
                            android.util.Log.d("MainAppScreen", "🔙 On Dashboard - minimizing app")
                            activity?.moveTaskToBack(true)
                        }
                        // Check if there's something to pop
                        navController.previousBackStackEntry != null && currentRoute != ScreenName.Dashboard -> {
                            android.util.Log.d("MainAppScreen", "🔙 Popping back stack from $currentRoute")
                            navController.popBackStack()
                        }
                        // Fallback case - navigate to dashboard
                        else -> {
                            android.util.Log.d("MainAppScreen", "🔙 Fallback - navigating to Dashboard")
                            navController.navigate(ScreenName.Dashboard) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainAppScreen", "❌ Error handling back press: ${e.message}", e)
                } finally {
                    backMutex.unlock()
                }
            } else {
                android.util.Log.w("MainAppScreen", "⚠️ Back button blocked - Navigation already in progress")
            }
        }
    }

    // 🔧 FIX: Removed pointer input blocking that was causing black/white screens
    // Instead, rely on isScreenReady checks in individual click handlers
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // AppNavigationDrawer - gestures gated on destination lifecycle
        AppNavigationDrawer(
            navController = navController,
            topLevelNavController = topLevelNavController,
            drawerState = drawerState,
            gesturesEnabled = isDestinationStable && currentRoute != ScreenName.BuildingMap, // Disable swipe on map
            onMenuClick = stableOnMenuClick
        ) {
            // The NavHost is INSIDE the drawer's content
            NavHost(
                navController = navController, 
                startDestination = startRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                // ⚡ MIUI FIX: Disable all navigation animations to prevent black screen
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
            
            composable(ScreenName.BuildingMap) {
                DeferredScreen {
                    BuildingMapScreen(
                        navController = navController,
                        onMenuClick = stableOnMenuClick
                    )
                }
            }

            // Define all your screens here
            // ⚡ PERFORMANCE FIX: Wrap Dashboard in DeferredScreen to prevent frame skips
            composable(ScreenName.Dashboard) {
                DeferredScreen {
                    DashboardScreen(
                        navController = navController,
                        onMenuClick = stableOnMenuClick
                    )
                }
            }
            // ⚡ PERFORMANCE FIX: Wrap Map in DeferredScreen (heavy rendering)
            composable(ScreenName.Map) {
                DeferredScreen {
                    MapScreen(navController = navController, onMenuClick = stableOnMenuClick)
                }
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

            composable(ScreenName.EtoLicenseStatus) {
                com.innovative.smis.ui.features.etolicense.EtoLicenseStatusScreen(
                    navController = navController
                )
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
                ComprehensiveSurveyScreen(navController = navController, bin = null)
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

/**
 * ⚡ PERFORMANCE FIX: Deferred Screen Wrapper
 * 
 * Forces heavy screens to wait until navigation animation completes before rendering.
 * This prevents "Skipped frames" errors and black screen caused by main thread blocking.
 * 
 * How it works:
 * 1. Shows lightweight loading spinner for 300ms (navigation animation time)
 * 2. GPU gets a frame immediately (prevents QueueBuffer timeout)
 * 3. After delay, renders the actual heavy screen content
 * 
 * Use this for:
 * - Dashboard (11+ items with complex UI)
 * - Map (heavy rendering)
 * - Any screen causing "Skipped frames" warnings
 */
@Composable
fun DeferredScreen(
    minDelay: Long = 300, // Standard nav animation time
    content: @Composable () -> Unit
) {
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Yield to allow nav animation to start smoothly
        delay(minDelay)
        isReady = true
        android.util.Log.d("DeferredScreen", "⚡ Screen ready after ${minDelay}ms delay")
    }

    if (isReady) {
        content()
    } else {
        // Render lightweight placeholder to prevent Black Screen / Timeout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
