package com.example.coustomerapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.coustomerapp.data.local.dao.*
import com.example.coustomerapp.data.local.entities.*

@Database(
    entities = [
        UserSessionEntity::class,
        CustomerProfileEntity::class,
        ServiceRequestEntity::class,
        DispatchRecordEntity::class,
        InverterGenerationSummaryEntity::class,
        InverterGenerationHistoryEntity::class,
        InvoiceEntity::class,
        SavedCardEntity::class,
        AmcVisit::class
    ],
    version = 4, // Upgraded version to support schema changes and trigger fallback migration
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun customerProfileDao(): CustomerProfileDao
    abstract fun serviceRequestDao(): ServiceRequestDao
    abstract fun dispatchRecordDao(): DispatchRecordDao
    abstract fun inverterGenerationSummaryDao(): InverterGenerationSummaryDao
    abstract fun inverterGenerationHistoryDao(): InverterGenerationHistoryDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun savedCardDao(): SavedCardDao
    abstract fun amcVisitDao(): AmcVisitDao
    abstract fun inverterDao(): InverterDao
}
