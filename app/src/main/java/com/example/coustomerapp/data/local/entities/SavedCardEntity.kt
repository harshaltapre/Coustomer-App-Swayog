package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cards")
data class SavedCardEntity(
    @PrimaryKey val cardNumberHash: String,
    val cardHolderName: String,
    val maskedCardNumber: String, // e.g. "•••• •••• •••• 4242"
    val expiryDate: String,
    val cardBrand: String // 'Visa', 'Mastercard', etc.
)
