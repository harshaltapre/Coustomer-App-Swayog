package com.example.coustomerapp.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "inverter_generation_history",
    primaryKeys = ["customerId", "period", "label"]
)
data class InverterGenerationHistoryEntity(
    val customerId: Int,
    val period: String, // 'realtime', 'daily', 'monthly', 'yearly'
    val label: String,  // Time, Date, Month, or Year label
    val powerValue: Double?, // Used for kW in real-time
    val generationValue: Double? // Used for kWh in historical views
)
