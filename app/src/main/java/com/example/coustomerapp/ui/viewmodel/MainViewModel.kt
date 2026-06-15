package com.example.coustomerapp.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.coustomerapp.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)

    private val _fontSize = MutableStateFlow(prefs.getString("font_size", "Normal") ?: "Normal")
    val fontSize: StateFlow<String> = _fontSize.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "font_size") {
            _fontSize.value = prefs.getString("font_size", "Normal") ?: "Normal"
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.getAccessToken() != null
    }

    fun logout() {
        sessionManager.logout()
    }
}
