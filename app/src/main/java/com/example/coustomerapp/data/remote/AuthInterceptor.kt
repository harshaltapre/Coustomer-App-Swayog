package com.example.coustomerapp.data.remote

import com.example.coustomerapp.util.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

class AuthInterceptor @Inject constructor(
    private val sessionManager: Provider<SessionManager>
) : Interceptor {

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val REFRESH_PATH = "api/v1/auth/refresh"
    }

    @Volatile
    private var isRefreshing = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for login and refresh endpoints
        val path = originalRequest.url.encodedPath
        if (path.contains("auth/login") || path.contains("auth/register")) {
            return chain.proceed(originalRequest)
        }

        val accessToken = sessionManager.get().getAccessToken()
        val authenticatedRequest = if (accessToken != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(authenticatedRequest)

        // If we get a 401 and we have a refresh token, attempt a silent refresh
        if (response.code == 401 && !isRefreshing) {
            val refreshToken = sessionManager.get().getRefreshToken()
            if (refreshToken != null) {
                synchronized(this) {
                    // Double-check inside synchronized block
                    if (isRefreshing) return response
                    isRefreshing = true
                }

                try {
                    val newTokens = attemptTokenRefresh(chain, refreshToken)
                    if (newTokens != null) {
                        sessionManager.get().saveTokens(newTokens.first, newTokens.second)

                        // Close the old 401 response body
                        response.close()

                        // Retry the original request with the new token
                        val retryRequest = originalRequest.newBuilder()
                            .removeHeader("Authorization")
                            .addHeader("Authorization", "Bearer ${newTokens.first}")
                            .build()
                        return chain.proceed(retryRequest)
                    } else {
                        // Refresh failed — clear session so the app redirects to login
                        sessionManager.get().logout()
                    }
                } finally {
                    isRefreshing = false
                }
            }
        }

        return response
    }

    /**
     * Sends a synchronous POST to /api/v1/auth/refresh with the current refresh token.
     * Uses the chain's connection for proper SSL and timeout handling.
     * Returns a Pair(accessToken, refreshToken) on success, or null on failure.
     */
    private fun attemptTokenRefresh(
        chain: Interceptor.Chain,
        refreshToken: String
    ): Pair<String, String>? {
        return try {
            val bodyJson = Gson().toJson(mapOf("refreshToken" to refreshToken))
            val requestBody = bodyJson.toRequestBody(JSON_MEDIA_TYPE)

            // Build the base URL from the original chain request
            val baseUrl = chain.request().url.newBuilder()
                .encodedPath("/$REFRESH_PATH")
                .query(null)
                .build()

            val refreshRequest = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            // Use the chain to proceed — this reuses the same OkHttpClient 
            // with proper SSL config, timeouts, and connection pool
            val refreshResponse = chain.proceed(refreshRequest)

            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                refreshResponse.close()
                if (responseBody != null) {
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    val parsed: Map<String, Any> = Gson().fromJson(responseBody, type)

                    @Suppress("UNCHECKED_CAST")
                    val data = parsed["data"] as? Map<String, Any>
                    val newAccess = data?.get("accessToken") as? String
                    val newRefresh = data?.get("refreshToken") as? String
                    if (newAccess != null && newRefresh != null) {
                        Pair(newAccess, newRefresh)
                    } else null
                } else null
            } else {
                refreshResponse.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
