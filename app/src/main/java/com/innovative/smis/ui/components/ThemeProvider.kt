package com.innovative.smis.ui.components

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.innovative.smis.ui.theme.SMISTheme
import com.innovative.smis.util.helper.PreferenceHelper
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThemeProvider(
    content: @Composable () -> Unit
) {
    val preferenceHelper: PreferenceHelper = org.koin.androidx.compose.get()
    
    // Remember theme mode from preferences
    var themeMode by rememberSaveable { mutableStateOf(preferenceHelper.themeMode) }
    
    // Listen for theme changes
    LaunchedEffect(Unit) {
        themeMode = preferenceHelper.themeMode
    }
    
    SMISTheme(
        themeMode = themeMode,
        content = content
    )
}