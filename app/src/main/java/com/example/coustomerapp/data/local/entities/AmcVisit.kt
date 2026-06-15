package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "amc_visits")
data class AmcVisit(
    @PrimaryKey val id: String,
    val technicianName: String,
    val technicianPhone: String,
    val scheduledDate: Long,
    val status: String, // scheduled, completed, cancelled
    val visitNumber: Int,
    val notes: String?,
    val timeSlot: String? = null
)
