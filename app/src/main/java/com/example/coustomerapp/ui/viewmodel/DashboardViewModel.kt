package com.example.coustomerapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.CustomerProfileEntity
import com.example.coustomerapp.data.local.entities.DispatchRecordEntity
import com.example.coustomerapp.data.local.entities.InverterGenerationSummaryEntity
import com.example.coustomerapp.data.remote.dto.CurrentWeather
import com.example.coustomerapp.data.repository.CustomerRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CustomerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine error", throwable)
        _isLoading.value = false
        _isRefreshing.value = false
    }

    val customerProfile: StateFlow<CustomerProfileEntity?> = repository.customerProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dispatches: StateFlow<List<DispatchRecordEntity>> = repository.dispatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inverterSummary: StateFlow<InverterGenerationSummaryEntity?> = customerProfile
        .flatMapLatest { profile ->
            if (profile != null) {
                repository.getInverterSummary(profile.id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Weather state
    private val _weatherState = MutableStateFlow<CurrentWeather?>(null)
    val weatherState: StateFlow<CurrentWeather?> = _weatherState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        initialLoad()
        syncFcmToken()
        observeProfileForWeather()
    }

    private fun syncFcmToken() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModelScope.launch(errorHandler) {
                            repository.updateFcmToken(task.result)
                        }
                    }
                }
            } else {
                Log.d(TAG, "Firebase not initialized: skipped syncFcmToken")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not initialized: ${e.message}")
        }
    }

    private fun observeProfileForWeather() {
        viewModelScope.launch(errorHandler) {
            customerProfile.filterNotNull().collect { profile ->
                if (profile.city.isNotBlank()) {
                    loadWeather(profile.city)
                }
            }
        }
    }

    fun loadWeather(city: String) {
        viewModelScope.launch(errorHandler) {
            val weather = repository.fetchWeatherForCity(city)
            _weatherState.value = weather
        }
    }

    private fun initialLoad() {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            try {
                repository.refreshProfile()
                repository.refreshDispatches()
                repository.refreshServiceRequests()
                repository.refreshAmcVisits()

                repository.getCustomerProfileDirect()?.let { profile ->
                    repository.refreshInverterTelemetry(profile.id)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch(errorHandler) {
            _isRefreshing.value = true
            try {
                repository.refreshProfile()
                repository.refreshDispatches()
                repository.refreshServiceRequests()
                repository.refreshAmcVisits()

                repository.getCustomerProfileDirect()?.let { profile ->
                    repository.refreshInverterTelemetry(profile.id)
                    loadWeather(profile.city.ifBlank { "Delhi" })
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
