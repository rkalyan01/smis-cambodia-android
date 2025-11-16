package com.innovative.smis.data.network

import com.innovative.smis.util.constants.Languages
import com.innovative.smis.util.helper.PreferenceHelper
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Intercepts API requests and appends /km suffix to dropdown endpoints when language is Khmer
 * 
 * The API provides translated dropdown options at language-specific endpoints:
 * - English: /api/emptying-scheduling/show-emptying-purpose
 * - Khmer: /api/emptying-scheduling/show-emptying-purpose/km
 */
class LanguageInterceptor(
    private val preferenceHelper: PreferenceHelper
) : Interceptor {

    companion object {
        /**
         * List of API endpoints that support language-specific responses
         */
        private val LOCALIZED_ENDPOINTS = listOf(
            "emptyings/show-additional-repairing",
            "emptyings/show-customer-type",
            "emptying-scheduling/additional-repair",
            "emptying-scheduling/show-emptying-purpose",
            "emptying-scheduling/show-notemptied-reason",
            "emptying-scheduling/show-emptied-nodate-reason",
            "site-preparation/show-additional-repairing",
            "containment/storage-type",
            "containment/storage-connection",
            "emptying-scheduling/show-issue-with-containment"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url
        
        // Get current language
        val currentLanguage = preferenceHelper.selectedLanguage
        
        // If language is Khmer and the endpoint supports localization, append /km
        if (currentLanguage == Languages.KHMER) {
            val path = url.encodedPath
            
            // Remove leading /api/ if present for matching
            val pathWithoutApiPrefix = path.removePrefix("/api/")
            
            // Check if this is a localized endpoint and doesn't already have /km suffix
            val isLocalizedEndpoint = LOCALIZED_ENDPOINTS.any { endpoint ->
                pathWithoutApiPrefix == endpoint && !path.endsWith("/km")
            }
            
            if (isLocalizedEndpoint) {
                // Append /km to the path
                val newUrl = url.newBuilder()
                    .encodedPath(path + "/km")
                    .build()
                
                val newRequest = originalRequest.newBuilder()
                    .url(newUrl)
                    .build()
                
                android.util.Log.d("LanguageInterceptor", "Khmer request: ${originalRequest.url} → ${newUrl}")
                return chain.proceed(newRequest)
            }
        }
        
        // For English or non-localized endpoints, proceed with original request
        return chain.proceed(originalRequest)
    }
}
