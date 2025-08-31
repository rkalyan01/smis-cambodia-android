package com.innovative.smis.util.localization

import android.content.Context
import android.content.res.Configuration
import com.innovative.smis.util.constants.Languages
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.helper.LocalizationHelper
import com.innovative.smis.util.helper.PreferenceHelper
import java.util.*

object LocalizationManager {
    
    fun setLocale(context: Context, languageCode: String): Context {
        return LocalizationHelper.setLocale(context, languageCode)
    }
    
    fun getCurrentLanguage(context: Context): String {
        val preferenceHelper = PreferenceHelper(context)
        // Use the standardized language code system
        return preferenceHelper.selectedLanguage
    }
    
    fun setCurrentLanguage(context: Context, languageCode: String) {
        val preferenceHelper = PreferenceHelper(context)
        preferenceHelper.selectedLanguage = languageCode
        // Also set the locale
        LocalizationHelper.setLocale(context, languageCode)
    }
    
    fun getLanguageCode(language: String): String {
        // Handle both old string format and new code format
        return when (language.lowercase()) {
            "khmer", "km" -> Languages.KHMER
            "english", "en" -> Languages.ENGLISH
            else -> Languages.ENGLISH
        }
    }
    
    fun getLanguageDisplayName(languageCode: String): String {
        return Languages.getLanguageByCode(languageCode)?.name ?: "English"
    }
}