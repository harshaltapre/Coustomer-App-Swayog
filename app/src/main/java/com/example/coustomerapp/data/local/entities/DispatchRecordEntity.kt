package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dispatch_records")
data class DispatchRecordEntity(
    @PrimaryKey val id: String,
    val customerId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val dispatchedAt: String,
    val notes: String
)
