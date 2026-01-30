package com.innovative.smis.ui.features.map

import androidx.compose.ui.graphics.Color

/**
 * Map layer polygon colors — must match exactly with
 * smis-cambodia-mobileapp/src/screens/root/imissmis/DashboardScreen.js
 * so Android and React Native maps look identical.
 */
object MapLayerColors {
    // Auxiliary: RN strokeColor = "#808080", fillColor = "rgba(0, 0, 0, 0.2)"
    const val AUXILIARY_STROKE = 0xFF808080
    const val AUXILIARY_FILL = 0x33000000

    // Surveyed: RN strokeColor = "#198754", fillColor = "rgba(25, 135, 84, 0.05)"
    const val SURVEYED_STROKE = 0xFF198754
    const val SURVEYED_FILL = 0x0D198754

    // Unsurveyed (default): RN strokeColor = "#007BFF", fillColor = "rgba(0, 123, 255, 0.05)"
    const val UNSURVEYED_STROKE = 0xFF007BFF
    const val UNSURVEYED_FILL = 0x0D007BFF

    // Highlighted: RN strokeColor = "#FFD700", fillColor = "rgba(255, 255, 0, 0.4)"
    const val HIGHLIGHT_STROKE = 0xFFFFD700
    const val HIGHLIGHT_FILL = 0x66FFFF00

    fun auxiliaryStroke() = Color(AUXILIARY_STROKE)
    fun auxiliaryFill() = Color(AUXILIARY_FILL)
    fun surveyedStroke() = Color(SURVEYED_STROKE)
    fun surveyedFill() = Color(SURVEYED_FILL)
    fun unsurveyedStroke() = Color(UNSURVEYED_STROKE)
    fun unsurveyedFill() = Color(UNSURVEYED_FILL)
    fun highlightStroke() = Color(HIGHLIGHT_STROKE)
    fun highlightFill() = Color(HIGHLIGHT_FILL)
}
