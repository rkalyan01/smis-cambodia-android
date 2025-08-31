package com.innovative.smis.util.helper

import android.content.Context
import android.content.res.Configuration
import java.util.*

object LocalizationHelper {
    
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun getCurrentLanguage(context: Context): String {
        return context.resources.configuration.locales[0].language
    }
    
    fun isRTL(languageCode: String): Boolean {
        return when (languageCode) {
            "ar", "fa", "he", "ur" -> true
            else -> false
        }
    }
    
    fun getDisplayName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "km" -> "ភាសាខ្មែរ"
            else -> languageCode
        }
    }
}