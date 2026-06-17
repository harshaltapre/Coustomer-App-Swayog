package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inverter_generation_summary")
data class InverterGenerationSummaryEntity(
    @PrimaryKey val customerId: Int,
    val dailyGeneration: Double,
    val totalGeneration: Double,
    val peakPower: Double,
    val currentPower: Double,
    val isSimulated: Boolean,
    val status: String,
    val lastUpdated: String
)
