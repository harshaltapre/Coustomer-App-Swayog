package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_requests")
data class ServiceRequestEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val serverId: Int? = null,
    val serviceType: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val preferredDate: String,
    val status: String = "pending",
    val isSynced: Boolean = false,
    val createdAt: String,
    val imagePath: String? = null
)
