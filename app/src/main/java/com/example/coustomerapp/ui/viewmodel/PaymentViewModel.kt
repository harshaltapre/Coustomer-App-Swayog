package com.example.coustomerapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.CustomerProfileEntity
import com.example.coustomerapp.data.local.entities.InvoiceEntity
import com.example.coustomerapp.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    val customerProfile: StateFlow<CustomerProfileEntity?> = repository.customerProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val invoices: StateFlow<List<InvoiceEntity>> = customerProfile
        .flatMapLatest { profile ->
            if (profile != null) {
                repository.getInvoices(profile.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outstandingBalance: StateFlow<Double> = invoices
        .map { list ->
            list.filter { it.paymentStatus.lowercase() == "pending" || it.paymentStatus.lowercase() == "failed" }
                .sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _paymentEvent = MutableStateFlow<PaymentEvent?>(null)
    val paymentEvent: StateFlow<PaymentEvent?> = _paymentEvent.asStateFlow()

    init {
        viewModelScope.launch {
            customerProfile.collectLatest { profile ->
                if (profile != null) {
                    repository.refreshInvoices(profile.id)
                }
            }
        }
    }

    fun refreshInvoices() {
        viewModelScope.launch {
            customerProfile.value?.let { profile ->
                repository.refreshInvoices(profile.id)
            }
        }
    }

    fun initiateRazorpayPayment(amount: Double) {
        viewModelScope.launch {
            try {
                val response = repository.createRazorpayOrder(amount)
                if (response != null) {
                    val orderId = response["orderId"] as? String
                    val keyId = response["keyId"] as? String
                    if (orderId != null && keyId != null) {
                        _paymentEvent.value = PaymentEvent.StartRazorpay(
                            orderId = orderId,
                            amount = amount,
                            key = keyId
                        )
                    }
                }
            } catch (e: Exception) {
                _paymentEvent.value = PaymentEvent.PaymentFailed(e.message ?: "Failed to initiate payment")
            }
        }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String) {
        viewModelScope.launch {
            val success = repository.verifyPayment(orderId, paymentId, signature)
            if (success) {
                _paymentEvent.value = PaymentEvent.PaymentSuccess
                refreshInvoices()
            } else {
                _paymentEvent.value = PaymentEvent.PaymentFailed("Payment verification failed")
            }
        }
    }

    fun handlePaymentFailure(message: String) {
        _paymentEvent.value = PaymentEvent.PaymentFailed(message)
    }

    fun clearEvent() {
        _paymentEvent.value = null
    }
}

sealed class PaymentEvent {
    data class StartRazorpay(val orderId: String, val amount: Double, val key: String) : PaymentEvent()
    object PaymentSuccess : PaymentEvent()
    data class PaymentFailed(val message: String) : PaymentEvent()
}
