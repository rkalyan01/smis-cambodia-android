package com.innovative.smis.ui.util

/**
 * UI model for dropdown and multiselect options with bilingual support
 * @param key The API key/ID to send to backend
 * @param labelResId The string resource ID for localized display
 * @param fallbackLabel English text to show if no translation exists
 */
data class OptionUiModel(
    val key: String,
    val labelResId: Int?,
    val fallbackLabel: String
)
