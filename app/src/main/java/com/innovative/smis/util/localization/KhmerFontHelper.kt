package com.innovative.smis.util.localization

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.innovative.smis.R

/**
 * Khmer font helper for proper Khmer text rendering
 * Provides Khmer-specific fonts and typography
 */
object KhmerFontHelper {
    
    // Khmer font family - using system fonts that support Khmer
    val khmerFontFamily = FontFamily.Default // Android system fonts support Khmer
    
    /**
     * Get typography with Khmer font support
     */
    fun getKhmerTypography(): Typography {
        return Typography(
            // Use default typography with proper Khmer font support
            // Android's default fonts handle Khmer characters well
        )
    }
    
    /**
     * Check if current language requires special font handling
     */
    fun requiresKhmerFont(languageCode: String): Boolean {
        return languageCode == "km"
    }
    
    /**
     * Format Khmer numbers if needed
     */
    fun formatNumber(number: String, languageCode: String): String {
        return if (languageCode == "km") {
            // Convert to Khmer numerals if needed
            convertToKhmerNumerals(number)
        } else {
            number
        }
    }
    
    /**
     * Convert Arabic numerals to Khmer numerals
     */
    private fun convertToKhmerNumerals(arabicNumber: String): String {
        val khmerNumerals = arrayOf("០", "១", "២", "៣", "៤", "៥", "៦", "៧", "៨", "៩")
        val arabicNumerals = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        
        var result = arabicNumber
        for (i in arabicNumerals.indices) {
            result = result.replace(arabicNumerals[i], khmerNumerals[i])
        }
        return result
    }
    
    /**
     * Get text direction for Khmer (Left-to-Right)
     */
    fun getTextDirection(languageCode: String): String {
        return "ltr" // Khmer is written left-to-right
    }
}