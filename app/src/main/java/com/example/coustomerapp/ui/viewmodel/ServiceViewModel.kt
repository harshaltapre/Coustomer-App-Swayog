package com.example.coustomerapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.AmcVisit
import com.example.coustomerapp.data.local.entities.CustomerProfileEntity
import com.example.coustomerapp.data.local.entities.ServiceRequestEntity
import com.example.coustomerapp.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ServiceViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ServiceViewModel"
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine error", throwable)
        _isLoading.value = false
        _isRefreshing.value = false
    }

    val serviceRequests: StateFlow<List<ServiceRequestEntity>> = repository.serviceRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amcVisits: StateFlow<List<AmcVisit>> = repository.amcVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerProfile: StateFlow<CustomerProfileEntity?> = repository.customerProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        initialLoad()
    }

    private fun initialLoad() {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            try {
                repository.refreshServiceRequests()
                repository.refreshAmcVisits()
                repository.refreshProfile()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshServiceData() {
        viewModelScope.launch(errorHandler) {
            _isRefreshing.value = true
            try {
                repository.refreshServiceRequests()
                repository.refreshAmcVisits()
                repository.refreshProfile()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun submitRequest(
        type: String,
        description: String,
        address: String,
        lat: Double,
        lon: Double,
        imageFiles: List<File> = emptyList()
    ) {
        viewModelScope.launch(errorHandler) {
            val mainImage = imageFiles.firstOrNull()
            repository.submitServiceRequest(
                title = type,
                description = description,
                address = address,
                latitude = lat,
                longitude = lon,
                localImage = mainImage
            )
        }
    }
}
