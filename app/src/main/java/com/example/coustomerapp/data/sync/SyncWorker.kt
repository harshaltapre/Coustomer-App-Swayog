package com.example.coustomerapp.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.coustomerapp.data.repository.CustomerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CustomerRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            repository.refreshProfile()
            repository.refreshDispatches()
            repository.refreshServiceRequests()
            repository.refreshAmcVisits()
            repository.getCustomerProfileDirect()?.let { profile ->
                repository.refreshInverterTelemetry(profile.id)
                repository.refreshInverterHistory(profile.id, "realtime")
                repository.refreshInverterHistory(profile.id, "daily")
                repository.refreshInverterHistory(profile.id, "monthly")
                repository.refreshInverterHistory(profile.id, "yearly")
                repository.refreshInvoices(profile.id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
