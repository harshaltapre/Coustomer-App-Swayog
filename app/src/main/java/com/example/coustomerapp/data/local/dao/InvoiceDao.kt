package com.example.coustomerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coustomerapp.data.local.entities.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY invoiceDate DESC")
    fun getAllInvoices(customerId: Int): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Query("UPDATE invoices SET paymentStatus = 'paid', amountPaid = amount WHERE id = :invoiceId")
    suspend fun markAsPaid(invoiceId: String)

    @Query("DELETE FROM invoices")
    suspend fun clearAll()
}
