package com.example.coustomerapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.CustomerProfileEntity
import com.example.coustomerapp.data.local.entities.InverterGenerationHistoryEntity
import com.example.coustomerapp.data.local.entities.InverterGenerationSummaryEntity
import com.example.coustomerapp.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AnalyticsViewModel"
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine error", throwable)
        _isLoading.value = false
    }

    val customerProfile: StateFlow<CustomerProfileEntity?> = repository.customerProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedPeriod = MutableStateFlow("realtime")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    val historyPoints: StateFlow<List<InverterGenerationHistoryEntity>> = combine(
        customerProfile,
        _selectedPeriod
    ) { profile, period ->
        Pair(profile, period)
    }.flatMapLatest { (profile, period) ->
        if (profile != null) {
            repository.getInverterHistory(profile.id, period)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inverterSummary: StateFlow<InverterGenerationSummaryEntity?> = customerProfile
        .flatMapLatest { profile ->
            if (profile != null) {
                repository.getInverterSummary(profile.id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Fetch initially
        refreshData()
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            try {
                repository.refreshProfile()
                repository.getCustomerProfileDirect()?.let { profile ->
                    repository.refreshInverterTelemetry(profile.id)
                    repository.refreshInverterHistory(profile.id, _selectedPeriod.value)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
