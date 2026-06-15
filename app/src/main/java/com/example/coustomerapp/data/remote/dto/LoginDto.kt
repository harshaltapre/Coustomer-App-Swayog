package com.example.coustomerapp.data.remote.dto

data class LoginRequest(
    val identifier: String,
    val password: String,
    val role: String = "CUSTOMER"
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)
