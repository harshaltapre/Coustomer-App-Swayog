package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val id: String,
    val loginId: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String?,
    val role: String,
    val isActive: Boolean,
    val accessToken: String,
    val refreshToken: String,
    val lastLoginTimestamp: Long
)
