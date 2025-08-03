package com.innovative.smis.di

import com.google.gson.GsonBuilder
import com.innovative.smis.BuildConfig
import com.innovative.smis.data.api.ApiService
import com.innovative.smis.data.repository.AuthRepository
import com.innovative.smis.ui.features.login.LoginViewModel
import com.innovative.smis.ui.features.map.MapViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { MapViewModel() }

}

val repositoryModule = module {
    single { AuthRepository(get()) }
}

val networkModule = module {
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
    }

    single {
        get<Retrofit>().create(ApiService::class.java)
    }
}
