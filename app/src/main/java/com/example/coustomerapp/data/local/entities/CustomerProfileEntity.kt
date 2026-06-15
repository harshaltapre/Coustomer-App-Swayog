package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_profile")
data class CustomerProfileEntity(
    @PrimaryKey val id: Int,
    val customerCode: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val city: String,
    val address: String,
    val systemSizeKw: Double,
    val installationDate: String,
    val warrantyExpiry: String?,
    val panelBrand: String?,
    val inverterBrand: String?,
    val inverterModel: String?,
    val amcStatus: String, // 'active', 'expired', 'none'
    val amcExpiryDate: String?,
    val status: String,
    val projectStage: Int, // 0 to 11 installation stage indicator
    val cleaningsPerMonth: Int,
    val completedVisits: Int, // completed AMC visits in the current month
    val pendingVisits: Int, // Cleanings pending for current month
    val clientType: String?,
    val consumerNumber: String?,
    val monthlyCleaningRate: Double?,
    val lastUpdatedLocally: Long
)
