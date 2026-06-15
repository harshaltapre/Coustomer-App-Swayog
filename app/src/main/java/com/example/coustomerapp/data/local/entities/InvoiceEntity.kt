package com.example.coustomerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val customerId: Int,
    val invoiceType: String, // 'installation', 'amc', 'cleaning'
    val amount: Double,
    val paymentStatus: String, // 'pending', 'paid', 'failed'
    val amountPaid: Double,
    val invoiceDate: String,
    val paymentDate: String?,
    val description: String?,
    val proofUrl: String?
)
