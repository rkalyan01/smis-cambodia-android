package com.innovative.smis.data.network

import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.helper.PreferenceHelper
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val prefs: PreferenceHelper) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = prefs.getAuthToken()

        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}