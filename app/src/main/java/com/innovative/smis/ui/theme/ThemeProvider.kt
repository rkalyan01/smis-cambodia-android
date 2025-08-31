package com.innovative.smis.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.innovative.smis.util.helper.PreferenceHelper

@Composable
fun ThemeProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferenceHelper = PreferenceHelper(context)

    var themeMode by remember { mutableStateOf(preferenceHelper.themeMode) }

    LaunchedEffect(Unit) {
        while (true) {
            val currentTheme = preferenceHelper.themeMode
            if (currentTheme != themeMode) {
                themeMode = currentTheme
            }
            kotlinx.coroutines.delay(100)
        }
    }

    SMISTheme(themeMode = themeMode) {
        content()
    }
}