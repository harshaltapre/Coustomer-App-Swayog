package com.example.coustomerapp.data.remote.dto

data class ApiResponse<T>(
    val data: T,
    val message: String? = null
)
