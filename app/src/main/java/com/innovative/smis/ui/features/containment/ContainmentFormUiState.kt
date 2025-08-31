package com.innovative.smis.ui.features.containment

data class ContainmentFormUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isLoadingDropdowns: Boolean = false,
    val isSubmitting: Boolean = false,
    
    // Form fields
    val toiletConnection: String = "Storage Tank",
    val selectedStorageType: String = "",
    val selectedStorageTypeKey: String = "",
    val otherTypeOfStorageTank: String = "",
    val selectedStorageConnection: String = "",
    val selectedStorageConnectionKey: String = "",
    val otherStorageTankConnection: String = "",
    val sizeOfStorageTankM3: String = "",
    val constructionYear: String = "",
    val accessibility: String = "",
    val accessibilityKey: String = "",
    val everEmptied: String = "",
    val everEmptiedKey: String = "",
    val lastEmptiedYear: String = "",
    
    // Dropdown options from API
    val storageTypeOptions: Map<String, String> = emptyMap(),
    val storageConnectionOptions: Map<String, String> = emptyMap(),
    
    // Error and success states
    val errorMessage: String? = null,
    val hasExistingData: Boolean = false
)