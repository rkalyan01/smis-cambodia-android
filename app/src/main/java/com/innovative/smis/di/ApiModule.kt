package com.innovative.smis.di

import com.innovative.smis.data.api.*
import com.innovative.smis.data.repository.*
import com.innovative.smis.ui.features.containment.ContainmentFormViewModel
import com.innovative.smis.ui.features.desludgingvehicle.DesludgingVehicleViewModel
import com.innovative.smis.ui.features.emptyingservice.EmptyingServiceFormViewModel
import com.innovative.smis.ui.features.taskmanagement.TaskManagementViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val apiModule = module {

    // API Services
    single<ContainmentApiService> {
        get<retrofit2.Retrofit>().create(ContainmentApiService::class.java)
    }

    single<DesludgingVehicleApiService> {
        get<retrofit2.Retrofit>().create(DesludgingVehicleApiService::class.java)
    }
    
    single<com.innovative.smis.data.api.PostponeApiService> {
        get<retrofit2.Retrofit>().create(com.innovative.smis.data.api.PostponeApiService::class.java)
    }

    // Repositories
    single<ContainmentRepository> {
        ContainmentRepository(
            apiService = get(),
            preferenceHelper = get(),
            formDao = get(),
            syncQueueDao = get()
        )
    }

    single<DesludgingVehicleRepository> {
        DesludgingVehicleRepository(get())
    }

    single<TaskManagementRepository> {
        TaskManagementRepository(
            get<TodoListApiService>(),
            get()
        )
    }

    single<AdditionalRepairingRepository> {
        AdditionalRepairingRepositoryImpl(
            api = get<LaravelApiService>(),
            dao = get()
        )
    }

    single<EmptyingServiceRepository> {
        EmptyingServiceRepository(
            apiService = get<LaravelApiService>(),
            containmentApiService = get<ContainmentApiService>(),
            postponeApiService = get<com.innovative.smis.data.api.PostponeApiService>(),
            formDao = get(),
            preferenceHelper = get(),
            context = androidContext()
        )
    }

    // ViewModels
    viewModel<ContainmentFormViewModel> {
        ContainmentFormViewModel(
            repository = get()
        )
    }

    viewModel<DesludgingVehicleViewModel> {
        DesludgingVehicleViewModel(
            repository = get(),
            preferenceHelper = get()
        )
    }

    viewModel<TaskManagementViewModel> {
        TaskManagementViewModel(
            repository = get()
        )
    }

    viewModel<EmptyingServiceFormViewModel> {
        EmptyingServiceFormViewModel(
            repository = get(),
            preferenceHelper = get()
        )
    }

    viewModel<com.innovative.smis.ui.features.additionalrepairing.AdditionalRepairingListViewModel> {
        com.innovative.smis.ui.features.additionalrepairing.AdditionalRepairingListViewModel(
            repository = get(),
            preferenceHelper = get()
        )
    }

    viewModel<com.innovative.smis.ui.features.additionalrepairing.AdditionalRepairingFormViewModel> {
        com.innovative.smis.ui.features.additionalrepairing.AdditionalRepairingFormViewModel(
            repository = get()
        )
    }
}