package com.example.coustomerapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.remote.ApiService
import com.example.coustomerapp.data.remote.dto.LoginRequest
import com.example.coustomerapp.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Login coroutine error", throwable)
        _loginState.value = LoginState.Error("Something went wrong. Please try again.")
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Please fill in all fields")
            return
        }
        
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch(errorHandler) {
            try {
                val request = LoginRequest(identifier = identifier.trim(), password = password.trim())
                val response = apiService.login(request)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val data = apiResponse?.data
                    if (data != null) {
                        sessionManager.saveTokens(data.accessToken, data.refreshToken)
                        sessionManager.saveCredentials(identifier, password)
                        _loginState.value = LoginState.Success
                    } else {
                        _loginState.value = LoginState.Error(apiResponse?.message ?: "Login failed. Please check your credentials.")
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Invalid email or password"
                        403 -> "Account is inactive. Contact support."
                        404 -> "Account not found"
                        500 -> "Server error. Please try again later."
                        else -> "Login failed (Error ${response.code()})"
                    }
                    _loginState.value = LoginState.Error(errorMsg)
                }
            } catch (e: java.net.SocketTimeoutException) {
                if (checkOfflineCredentials(identifier, password)) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Connection timed out. Check your network.")
                }
            } catch (e: java.net.UnknownHostException) {
                if (checkOfflineCredentials(identifier, password)) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("No internet connection. Please check your WiFi or data.")
                }
            } catch (e: java.net.ConnectException) {
                if (checkOfflineCredentials(identifier, password)) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Cannot reach server. Please try again later.")
                }
            } catch (e: Exception) {
                if (checkOfflineCredentials(identifier, password)) {
                    _loginState.value = LoginState.Success
                } else {
                    Log.e(TAG, "Login failed", e)
                    _loginState.value = LoginState.Error("Connection failed. Please try again.")
                }
            }
        }
    }

    private fun checkOfflineCredentials(identifier: String, password: String): Boolean {
        val savedId = sessionManager.getSavedLoginId()
        val savedPass = sessionManager.getSavedPass()
        return savedId != null && savedPass != null &&
                savedId.trim().lowercase() == identifier.trim().lowercase() &&
                savedPass.trim() == password.trim()
    }
    
    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    fun setErrorMessage(message: String) {
        _loginState.value = LoginState.Error(message)
    }

    fun loginWithSavedCredentials() {
        val id = sessionManager.getSavedLoginId()
        val pass = sessionManager.getSavedPass()
        if (id != null && pass != null) {
            login(id, pass)
        } else {
            _loginState.value = LoginState.Error("No saved credentials found. Please login once.")
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
