package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_requests",
    indices = [Index(value = ["id"]), Index(value = ["isSynced"])]
)
data class ServiceRequestEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val id: Int?, // Null if not yet sync'd with backend
    val customerId: Int,
    val title: String,
    val description: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val status: String, // 'pending', 'assigned', 'completed'
    val scheduledDate: String?,
    val scheduledTime: String?,
    val createdAt: String,
    val isSynced: Boolean = true,
    val pendingDelete: Boolean = false,
    val localImagePath: String? = null // Holds path of captured photo for compression/upload
)
