package com.example.coustomerapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val TAG = "SessionManager"
        private const val PREF_NAME = "swayog_session"
        private const val FALLBACK_PREF_NAME = "swayog_session_fallback"
    }

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "EncryptedSharedPreferences failed, using fallback", e)
        // Fallback to regular SharedPreferences to prevent crash
        context.getSharedPreferences(FALLBACK_PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    fun saveCredentials(loginId: String, pass: String) {
        sharedPreferences.edit()
            .putString("saved_login_id", loginId)
            .putString("saved_pass", pass)
            .apply()
    }

    fun getSavedLoginId(): String? = sharedPreferences.getString("saved_login_id", null)
    fun getSavedPass(): String? = sharedPreferences.getString("saved_pass", null)

    fun getAccessToken(): String? = sharedPreferences.getString("access_token", null)

    fun getRefreshToken(): String? = sharedPreferences.getString("refresh_token", null)

    fun saveFcmToken(token: String) {
        sharedPreferences.edit().putString("fcm_token", token).apply()
    }

    fun getFcmToken(): String? = sharedPreferences.getString("fcm_token", null)

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }
}
