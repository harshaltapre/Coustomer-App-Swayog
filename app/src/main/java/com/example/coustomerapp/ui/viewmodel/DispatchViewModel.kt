package com.example.coustomerapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.DispatchRecordEntity
import com.example.coustomerapp.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DispatchViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DispatchViewModel"
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine error", throwable)
        _isLoading.value = false
        _isRefreshing.value = false
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val dispatches: StateFlow<List<DispatchRecordEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.dispatches
            } else {
                repository.searchDispatches(query.trim())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                repository.refreshDispatches()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun refreshDispatches() {
        viewModelScope.launch(errorHandler) {
            _isRefreshing.value = true
            try {
                repository.refreshDispatches()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
