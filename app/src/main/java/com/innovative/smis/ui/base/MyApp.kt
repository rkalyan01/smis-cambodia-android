package com.innovative.smis.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.innovative.smis.ui.features.login.LoginScreen
import com.innovative.smis.ui.features.map.MapScreen
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.helper.PreferenceHelper

/**
 * The root composable that sets up the navigation graph for the entire app.
 */
@Composable
fun MyApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferenceHelper = PreferenceHelper(context)

    // Check the IS_LOGIN flag to determine the start destination.
    val isLoggedIn = preferenceHelper.getValue(PrefConstant.IS_LOGIN, false) as Boolean
    val startDestination = if (isLoggedIn) {
        ScreenName.Map
    } else {
        ScreenName.Login
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ScreenName.Login) {
            LoginScreen(navController = navController)
        }
        composable(ScreenName.Map) {
            MapScreen(navController = navController)
        }
        // Add your other composable routes here
    }
}
