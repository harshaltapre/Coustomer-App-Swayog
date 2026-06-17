package com.example.coustomerapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.ServiceRequestEntity
import com.example.coustomerapp.data.repository.CustomerRepository
import com.example.coustomerapp.data.sync.ServiceRequestSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ServiceRequestViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    val allRequests: StateFlow<List<ServiceRequestEntity>> =
        repository.getAllServiceRequests()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun submitRequest(
        context: Context,
        serviceType: String,
        description: String,
        address: String,
        latitude: Double,
        longitude: Double,
        preferredDate: String,
        imageFile: File? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val entity = ServiceRequestEntity(
                serviceType   = serviceType,
                description   = description,
                address       = address,
                latitude      = latitude,
                longitude     = longitude,
                preferredDate = preferredDate,
                createdAt     = now,
                isSynced      = false,
                imagePath     = imageFile?.absolutePath
            )
            repository.saveServiceRequestLocally(entity)
            ServiceRequestSyncWorker.enqueue(context)
            _isSubmitting.value = false
            _submitSuccess.value = true
        }
    }

    fun resetSubmitState() { _submitSuccess.value = false }
}
