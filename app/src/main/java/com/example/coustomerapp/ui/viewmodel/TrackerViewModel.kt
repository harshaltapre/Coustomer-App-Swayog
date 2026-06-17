package com.example.coustomerapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.InverterGenerationHistoryEntity
import com.example.coustomerapp.data.local.entities.InverterGenerationSummaryEntity
import com.example.coustomerapp.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("TrackerViewModel", "Error", throwable)
        _isLoading.value = false
        _isRefreshing.value = false
    }

    private val _selectedPeriod  = MutableStateFlow("realtime")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _isLoading       = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing    = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _historyPoints   = MutableStateFlow<List<InverterGenerationHistoryEntity>>(emptyList())
    val historyPoints: StateFlow<List<InverterGenerationHistoryEntity>> = _historyPoints.asStateFlow()

    private val _inverterSummary = MutableStateFlow<InverterGenerationSummaryEntity?>(null)
    val inverterSummary: StateFlow<InverterGenerationSummaryEntity?> = _inverterSummary.asStateFlow()

    init { loadTelemetry() }

    private fun loadTelemetry() {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            val profile = repository.getCustomerProfileDirect()
            profile?.let { p ->
                repository.refreshInverterTelemetry(p.id)
                repository.getInverterSummary(p.id).collect { sum ->
                    _inverterSummary.value = sum
                }
            }
            fetchHistory()
        }
    }

    private fun fetchHistory() {
        viewModelScope.launch(errorHandler) {
            val profile = repository.getCustomerProfileDirect() ?: return@launch
            val period      = _selectedPeriod.value
            // Backend only accepts: realtime | daily | yearly
            // weekly and monthly both use "daily" data, filtered client-side
            val fetchPeriod = if (period == "weekly" || period == "monthly") "daily" else period
            repository.refreshInverterHistory(profile.id, fetchPeriod)
            repository.getInverterHistory(profile.id, fetchPeriod).collect { list ->
                _historyPoints.value = when (period) {
                    "weekly"  -> list.takeLast(7)
                    "monthly" -> list  // full month — backend already scopes it
                    else      -> list
                }
                _isLoading.value = false
            }
        }
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        fetchHistory()
    }

    fun refreshTracker() {
        viewModelScope.launch(errorHandler) {
            _isRefreshing.value = true
            val profile = repository.getCustomerProfileDirect() ?: return@launch
            repository.refreshInverterTelemetry(profile.id)
            val fetchPeriod = if (_selectedPeriod.value in listOf("weekly","monthly")) "daily" else _selectedPeriod.value
            repository.refreshInverterHistory(profile.id, fetchPeriod)
            _isRefreshing.value = false
        }
    }
}
