package com.example.coustomerapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.CustomerProfileEntity
import com.example.coustomerapp.data.local.entities.SavedCardEntity
import com.example.coustomerapp.data.repository.CustomerRepository
import com.example.coustomerapp.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: CustomerRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val customerProfile: StateFlow<CustomerProfileEntity?> = repository.customerProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedCards: StateFlow<List<SavedCardEntity>> = repository.savedCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _themeMode = MutableStateFlow("Dark Mode") // Default as per spec
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _fontSize = MutableStateFlow("Normal")
    val fontSize: StateFlow<String> = _fontSize.asStateFlow()

    private val _is2FAEnabled = MutableStateFlow(false)
    val is2FAEnabled: StateFlow<Boolean> = _is2FAEnabled.asStateFlow()

    private val _isSessionTimeoutEnabled = MutableStateFlow(true)
    val isSessionTimeoutEnabled: StateFlow<Boolean> = _isSessionTimeoutEnabled.asStateFlow()

    private val _profileAvatarPath = MutableStateFlow<String?>(null)
    val profileAvatarPath: StateFlow<String?> = _profileAvatarPath.asStateFlow()

    init {
        // Load settings from Shared Preferences
        val prefs = context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString("theme_mode", "Dark Mode") ?: "Dark Mode"
        _fontSize.value = prefs.getString("font_size", "Normal") ?: "Normal"
        _is2FAEnabled.value = prefs.getBoolean("2fa_enabled", false)
        _isSessionTimeoutEnabled.value = prefs.getBoolean("session_timeout_enabled", true)
        _profileAvatarPath.value = prefs.getString("avatar_path", null)
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode).apply()
    }

    fun setFontSize(size: String) {
        _fontSize.value = size
        context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
            .edit().putString("font_size", size).apply()
    }

    fun set2FAEnabled(enabled: Boolean) {
        _is2FAEnabled.value = enabled
        context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("2fa_enabled", enabled).apply()
    }

    fun setSessionTimeoutEnabled(enabled: Boolean) {
        _isSessionTimeoutEnabled.value = enabled
        context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("session_timeout_enabled", enabled).apply()
    }

    fun saveAvatarImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@launch

                // Crop to 240x240 px center bounds
                val size = originalBitmap.width.coerceAtMost(originalBitmap.height)
                val x = (originalBitmap.width - size) / 2
                val y = (originalBitmap.height - size) / 2
                val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)
                val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, 240, 240, true)

                val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.png")
                val out = FileOutputStream(file)
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()

                _profileAvatarPath.value = file.absolutePath
                context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
                    .edit().putString("avatar_path", file.absolutePath).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSavedCard(cardHolderName: String, cardNumber: String, expiryDate: String) {
        viewModelScope.launch {
            val sanitizedNumber = cardNumber.replace(" ", "")
            if (sanitizedNumber.length < 12) return@launch

            // Generate primary key hash
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(sanitizedNumber.toByteArray(Charsets.UTF_8))
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }

            // Mask number
            val last4 = sanitizedNumber.takeLast(4)
            val masked = "•••• •••• •••• $last4"

            // Card brand determination
            val brand = when {
                sanitizedNumber.startsWith("4") -> "Visa"
                sanitizedNumber.startsWith("51") || sanitizedNumber.startsWith("52") ||
                        sanitizedNumber.startsWith("53") || sanitizedNumber.startsWith("54") ||
                        sanitizedNumber.startsWith("55") -> "Mastercard"
                sanitizedNumber.startsWith("34") || sanitizedNumber.startsWith("37") -> "Amex"
                else -> "Card"
            }

            val entity = SavedCardEntity(
                cardNumberHash = hashString,
                cardHolderName = cardHolderName,
                maskedCardNumber = masked,
                expiryDate = expiryDate,
                cardBrand = brand
            )
            repository.saveCard(entity)
        }
    }

    fun removeSavedCard(card: SavedCardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun signOutEverywhere(onLogoutRedirect: () -> Unit) {
        viewModelScope.launch {
            try {
                // Call server to invalidate tokens if applicable, then clear database
                repository.clearUserSession()
                sessionManager.logout()
                onLogoutRedirect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
