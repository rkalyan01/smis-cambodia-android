package com.innovative.smis.util.constants

/**
 * Language constants and locale management
 */
object Languages {
    const val ENGLISH = "en"
    const val KHMER = "km"
    
    data class Language(
        val code: String,
        val name: String,
        val nativeName: String
    )
    
    val SUPPORTED_LANGUAGES = listOf(
        Language(ENGLISH, "English", "English"),
        Language(KHMER, "Khmer", "ភាសាខ្មែរ")
    )
    
    fun getLanguageByCode(code: String): Language? {
        return SUPPORTED_LANGUAGES.find { it.code == code }
    }
}