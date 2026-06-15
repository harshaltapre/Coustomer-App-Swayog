package com.example.coustomerapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.CustomerProfileEntity
import com.example.coustomerapp.data.local.entities.DispatchRecordEntity
import com.example.coustomerapp.data.local.entities.InverterGenerationSummaryEntity
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
    private val _weatherTemp = MutableStateFlow("--")
    val weatherTemp: StateFlow<String> = _weatherTemp.asStateFlow()

    private val _weatherWind = MutableStateFlow("--")
    val weatherWind: StateFlow<String> = _weatherWind.asStateFlow()

    private val _weatherCondition = MutableStateFlow("Loading...")
    val weatherCondition: StateFlow<String> = _weatherCondition.asStateFlow()

    private val _weatherIcon = MutableStateFlow("☀️")
    val weatherIcon: StateFlow<String> = _weatherIcon.asStateFlow()

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
            customerProfile.filterNotNull().first().let { profile ->
                fetchWeather(profile.city.ifBlank { "Delhi" })
            }
        }
    }

    private suspend fun fetchWeather(city: String) {
        withContext(Dispatchers.IO) {
            try {
                val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
                val url = "https://wttr.in/$encodedCity?format=j1"
                val json = URL(url).readText()
                val root = JSONObject(json)
                val current = root.getJSONArray("current_condition").getJSONObject(0)

                val tempC = current.getString("temp_C")
                val windKmph = current.getString("windspeedKmph")
                val weatherDesc = current.getJSONArray("weatherDesc").getJSONObject(0).getString("value")
                val weatherCode = current.optString("weatherCode", "113")

                _weatherTemp.value = "${tempC}°C"
                _weatherWind.value = "$windKmph km/h"
                _weatherCondition.value = weatherDesc

                // Map weather code to emoji
                _weatherIcon.value = when {
                    weatherCode == "113" -> "☀️"
                    weatherCode == "116" -> "⛅"
                    weatherCode in listOf("119", "122") -> "☁️"
                    weatherCode in listOf("176", "263", "266", "293", "296", "299", "302", "305", "308") -> "🌧️"
                    weatherCode in listOf("200", "386", "389", "392", "395") -> "⛈️"
                    weatherCode in listOf("227", "230") -> "❄️"
                    weatherCode in listOf("143", "248", "260") -> "🌫️"
                    else -> "🌤️"
                }

                Log.d(TAG, "Weather fetched: $tempC°C, $windKmph km/h, $weatherDesc")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch weather", e)
                _weatherCondition.value = "Unavailable"
                _weatherIcon.value = "🌤️"
            }
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
                    fetchWeather(profile.city.ifBlank { "Delhi" })
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
