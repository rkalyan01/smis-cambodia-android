package com.innovative.smis.di

import com.innovative.smis.BuildConfig
import com.innovative.smis.data.api.ApiService
import com.innovative.smis.data.api.TodoListApiService
import com.innovative.smis.data.api.EmptyingSchedulingApiService
import com.innovative.smis.data.api.SitePreparationApiService
import com.innovative.smis.data.api.EmptyingServiceApiService
import com.innovative.smis.data.network.AuthInterceptor
import com.innovative.smis.data.repository.AuthRepository
import com.innovative.smis.data.repository.TodoListRepository
import com.innovative.smis.data.repository.EmptyingSchedulingRepository
import com.innovative.smis.data.repository.SitePreparationRepository
import com.innovative.smis.data.repository.EmptyingServiceRepository
import androidx.room.Room
import androidx.room.RoomDatabase
import com.innovative.smis.util.constants.ApiConstants
import com.innovative.smis.data.api.AuthApiService
import com.innovative.smis.data.api.BuildingSurveyApiService
import com.innovative.smis.data.api.EmptyingApiService
import com.innovative.smis.data.api.LaravelApiService
import com.innovative.smis.data.api.ContainmentApiService
import com.innovative.smis.data.local.MemoryTaskDao
import com.innovative.smis.data.local.dao.EmptyingSchedulingFormDao
import com.innovative.smis.data.local.dao.SitePreparationFormDao
import com.innovative.smis.data.local.dao.EmptyingServiceFormDao
import com.innovative.smis.data.local.dao.ContainmentFormDao
import com.innovative.smis.data.local.dao.TaskDao
import com.innovative.smis.data.local.dao.TodoItemDao
import com.innovative.smis.data.local.database.SMISDatabase
import com.innovative.smis.data.local.offline.OfflineManager
import com.innovative.smis.data.repository.*
import com.innovative.smis.data.repository.WorkflowRepository
import com.innovative.smis.data.local.dao.WorkflowStepDao
import com.innovative.smis.data.local.dao.SyncQueueDao
import com.innovative.smis.data.repository.BuildingSurveyRepository
import com.innovative.smis.data.repository.EmptyingRepository
import com.innovative.smis.data.repository.ContainmentRepository
import com.innovative.smis.domain.repository.TaskRepository
import com.innovative.smis.ui.features.buildingsurvey.BuildingSurveyViewModel
import com.innovative.smis.ui.features.dashboard.DashboardViewModel
import com.innovative.smis.ui.features.emptying.EmptyingFormViewModel
import com.innovative.smis.ui.features.emptyingscheduling.EmptyingSchedulingFormViewModel
import com.innovative.smis.ui.features.login.LoginViewModel
import com.innovative.smis.ui.features.map.MapViewModel
import com.innovative.smis.ui.features.todolist.TodoListViewModel
import com.innovative.smis.ui.features.emptyingscheduling.EmptyingSchedulingViewModel
import com.innovative.smis.util.helper.PreferenceHelper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.koin.android.ext.koin.androidApplication
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    single { PreferenceHelper(androidContext()) }

    // Cache Management
    single { com.innovative.smis.data.local.cache.OfflineCacheManager(get()) }

    // Workflow Repository for new eto_id based API calls
    single { WorkflowRepository(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { MapViewModel(get()) }
    viewModel { DashboardViewModel(get<TodoListRepository>(), get<PreferenceHelper>()) }
    viewModel { EmptyingFormViewModel(get()) }
    viewModel { BuildingSurveyViewModel() }
    viewModel { com.innovative.smis.ui.features.dashboard.LogoutViewModel(androidContext(), get<TaskRepository>(), get<AuthRepository>()) }
    viewModel { com.innovative.smis.ui.features.map.OfflineMapViewModel(get<com.innovative.smis.data.local.offline.OfflineMapManager>()) }
    viewModel { com.innovative.smis.ui.features.settings.SettingsViewModel(androidContext().applicationContext as android.app.Application, get(), get(), get(), get(), get()) }
    viewModel { TodoListViewModel(get<TodoListRepository>()) }
    viewModel { EmptyingSchedulingViewModel(get<EmptyingSchedulingRepository>()) }
    viewModel { EmptyingSchedulingFormViewModel(get<EmptyingSchedulingRepository>()) }
    viewModel { com.innovative.smis.ui.features.sitepreparation.SitePreparationViewModel(get<WorkflowRepository>()) }
    viewModel { com.innovative.smis.ui.features.sitepreparation.SitePreparationFormViewModel(get<SitePreparationRepository>()) }
    viewModel { com.innovative.smis.ui.features.emptyingservice.EmptyingServiceViewModel(get<WorkflowRepository>()) }
    viewModel { com.innovative.smis.ui.features.emptyingservice.EmptyingServiceFormViewModel(get()) }
    viewModel { com.innovative.smis.ui.features.buildingsurvey.ComprehensiveSurveyViewModel() }
    viewModel { com.innovative.smis.ui.features.desludgingvehicle.DesludgingVehicleViewModel(get()) }
}

val databaseModule = module {
    single {
        // ✅ PERFORMANCE: Allow database queries on main thread during startup to prevent ANRs
        // and enable WAL mode for better concurrent access
        Room.databaseBuilder(
            androidContext(),
            SMISDatabase::class.java,
            SMISDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(true)
            .allowMainThreadQueries() // Allows critical startup queries on main thread
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // Better performance
            .build()
    }
    single<TaskDao> { get<SMISDatabase>().taskDao() }
    single<WorkflowStepDao> { get<SMISDatabase>().workflowStepDao() }
    single { get<SMISDatabase>().buildingSurveyDao() }
    single { get<SMISDatabase>().userDao() }
    single { get<SMISDatabase>().surveyDropdownDao() }
    single { get<SMISDatabase>().wfsBuildingDao() }

    single<SyncQueueDao> { get<SMISDatabase>().syncQueueDao() }
    single<TodoItemDao> { get<SMISDatabase>().todoItemDao() }
    single<EmptyingSchedulingFormDao> { get<SMISDatabase>().emptyingSchedulingFormDao() }
    single<SitePreparationFormDao> { get<SMISDatabase>().sitePreparationFormDao() }
    single<EmptyingServiceFormDao> { get<SMISDatabase>().emptyingServiceFormDao() }
    single<ContainmentFormDao> { get<SMISDatabase>().containmentFormDao() }

    // Offline Map DAOs
    single { get<SMISDatabase>().offlineMapTileDao() }
    single { get<SMISDatabase>().offlineBuildingPolygonDao() }
    single { get<SMISDatabase>().offlineMapAreaDao() }
    single { get<SMISDatabase>().offlinePOIDao() }
}

val offlineModule = module {
    single {
        OfflineManager(
            context = androidContext(),
            taskDao = get(),
            buildingSurveyDao = get(),
            workflowStepDao = get(),

            syncQueueDao = get(),
            surveyDropdownDao = get(),
            wfsBuildingDao = get(),
            emptyingSchedulingFormDao = get(),
            sitePreparationFormDao = get(),
            emptyingServiceFormDao = get(),
            emptyingApiService = get(),
            buildingSurveyApiService = get(),
            emptyingSchedulingApiService = get(),
            sitePreparationApiService = get(),
            emptyingServiceApiService = get(),
            moshi = get()
        )
    }
    single {
        com.innovative.smis.data.local.offline.OfflineMapManager(
            context = androidContext(),
            mapTileDao = get(),
            buildingPolygonDao = get(),
            mapAreaDao = get(),
            poiDao = get()
        )
    }
}

val repositoryModule = module {
    single { AuthRepository(get(), get()) }
    single<TaskRepository> { OfflineTaskRepositoryImpl(get<OfflineManager>(), get<TaskDao>(), get<WorkflowStepDao>(), get<SyncQueueDao>()) }
    single<com.innovative.smis.data.local.TaskDao> { MemoryTaskDao() }
    single<EmptyingRepository> { EmptyingRepositoryImpl(get()) }
    single<BuildingSurveyRepository> { BuildingSurveyRepositoryImpl(get()) }

    single {
        TodoListRepository(
            apiService = get<TodoListApiService>(),
            dao = get<TodoItemDao>(),
            preferenceHelper = get<PreferenceHelper>()
        )
    }

    single {
        EmptyingSchedulingRepository(
            apiService = get<EmptyingSchedulingApiService>(),
            formDao = get<EmptyingSchedulingFormDao>(),
            syncQueueDao = get<SyncQueueDao>(),
            todoItemDao = get<TodoItemDao>(),
            preferenceHelper = get<PreferenceHelper>()
        )
    }

    single {
        SitePreparationRepository(
            apiService = get<SitePreparationApiService>(),
            formDao = get<SitePreparationFormDao>(),
            syncQueueDao = get<SyncQueueDao>(),
            todoItemDao = get<TodoItemDao>(),
            preferenceHelper = get<PreferenceHelper>()
        )
    }

    single<com.innovative.smis.data.repository.DesludgingVehicleRepository> {
        com.innovative.smis.data.repository.DesludgingVehicleRepository(get())
    }


}

val networkModule = module {
    single { AuthInterceptor(get()) }
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(get<AuthInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    single {
        Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>().create(ApiService::class.java) }
    single<EmptyingApiService> { get<Retrofit>().create(EmptyingApiService::class.java) }
    single<BuildingSurveyApiService> { get<Retrofit>().create(BuildingSurveyApiService::class.java) }
    single<AuthApiService> { get<Retrofit>().create(AuthApiService::class.java) }
    single<TodoListApiService> { get<Retrofit>().create(TodoListApiService::class.java) }
    single<EmptyingSchedulingApiService> { get<Retrofit>().create(EmptyingSchedulingApiService::class.java) }
    single<SitePreparationApiService> { get<Retrofit>().create(SitePreparationApiService::class.java) }
    single<EmptyingServiceApiService> { get<Retrofit>().create(EmptyingServiceApiService::class.java) }
    single<com.innovative.smis.data.api.DesludgingVehicleApiService> { get<Retrofit>().create(com.innovative.smis.data.api.DesludgingVehicleApiService::class.java) }
    single<com.innovative.smis.data.api.LaravelApiService> { get<Retrofit>().create(com.innovative.smis.data.api.LaravelApiService::class.java) }

    single<com.innovative.smis.data.repository.ApplicationRepository> {
        com.innovative.smis.data.repository.ApplicationRepositoryImpl(apiService = get<ApiService>())
    }
}
