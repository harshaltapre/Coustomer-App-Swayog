package com.example.coustomerapp.data.repository

import android.content.Context
import androidx.work.*
import com.example.coustomerapp.data.local.dao.*
import com.example.coustomerapp.data.local.entities.*
import com.example.coustomerapp.data.remote.ApiService
import com.example.coustomerapp.data.sync.ServiceRequestSyncWorker
import com.example.coustomerapp.util.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val userSessionDao: UserSessionDao,
    private val customerProfileDao: CustomerProfileDao,
    private val serviceRequestDao: ServiceRequestDao,
    private val dispatchRecordDao: DispatchRecordDao,
    private val inverterGenerationSummaryDao: InverterGenerationSummaryDao,
    private val inverterGenerationHistoryDao: InverterGenerationHistoryDao,
    private val invoiceDao: InvoiceDao,
    private val savedCardDao: SavedCardDao,
    private val amcVisitDao: AmcVisitDao,
    private val sessionManager: SessionManager
) {
    val customerProfile: Flow<CustomerProfileEntity?> = customerProfileDao.getCustomerProfile()
    val dispatches: Flow<List<DispatchRecordEntity>> = dispatchRecordDao.getAllDispatches()
    fun searchDispatches(query: String): Flow<List<DispatchRecordEntity>> = dispatchRecordDao.searchDispatches(query)

    val serviceRequests: Flow<List<ServiceRequestEntity>> = serviceRequestDao.getAllServiceRequests()
    val amcVisits: Flow<List<AmcVisit>> = amcVisitDao.getAllAmcVisits()
    val savedCards: Flow<List<SavedCardEntity>> = savedCardDao.getSavedCards()

    fun getInvoices(customerId: Int): Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices(customerId)

    fun getInverterSummary(customerId: Int): Flow<InverterGenerationSummaryEntity?> =
        inverterGenerationSummaryDao.getSummary(customerId)

    fun getInverterHistory(customerId: Int, period: String): Flow<List<InverterGenerationHistoryEntity>> =
        inverterGenerationHistoryDao.getHistory(customerId, period)

    suspend fun refreshProfile() {
        try {
            val response = apiService.getProfile()
            val data = if (response.isSuccessful) {
                response.body()?.data
            } else {
                apiService.getInstallationTracker().body()?.data
            }

            data?.let { dto ->
                val profile = CustomerProfileEntity(
                    id = dto.id,
                    customerCode = dto.customerCode,
                    fullName = dto.name,
                    email = dto.email,
                    phoneNumber = dto.phoneNumber ?: "",
                    city = dto.city ?: "",
                    address = dto.address,
                    systemSizeKw = dto.systemSizeKw,
                    installationDate = dto.installationDate ?: "",
                    warrantyExpiry = dto.warrantyExpiry,
                    panelBrand = dto.panelBrand ?: "Standard",
                    inverterBrand = dto.inverterBrand ?: "Standard",
                    inverterModel = dto.inverterModel ?: "Standard",
                    amcStatus = dto.amcStatus ?: "none",
                    amcExpiryDate = dto.amcExpiryDate,
                    status = dto.liveStatus,
                    projectStage = dto.installationStage,
                    cleaningsPerMonth = dto.cleaningsPerMonth ?: 2,
                    completedVisits = dto.completedVisits ?: 0,
                    pendingVisits = dto.pendingVisits ?: 0,
                    clientType = dto.clientType,
                    consumerNumber = dto.consumerNumber,
                    monthlyCleaningRate = dto.monthlyCleaningRate,
                    lastUpdatedLocally = System.currentTimeMillis()
                )
                customerProfileDao.insertProfile(profile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val format = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                    if (pattern.endsWith("'Z'")) {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                }
                val date = format.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Ignore and try next pattern
            }
        }
        return System.currentTimeMillis()
    }

    suspend fun refreshDispatches() {
        try {
            val response = apiService.getDispatches()
            if (response.isSuccessful) {
                response.body()?.data?.let { dtos ->
                    val records = dtos.map { dto ->
                        DispatchRecordEntity(
                            id = dto.id,
                            customerId = dto.customerId,
                            itemId = dto.itemId,
                            itemName = dto.itemName,
                            quantity = dto.quantity,
                            pricePerUnit = dto.pricePerUnit,
                            dispatchedAt = dto.dispatchedAt,
                            notes = dto.notes ?: ""
                        )
                    }
                    dispatchRecordDao.clearAll()
                    dispatchRecordDao.insertAll(records)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshServiceRequests() {
        try {
            val response = apiService.getServiceRequests()
            if (response.isSuccessful) {
                val profileDirect = customerProfileDao.getCustomerProfileDirect()
                val customerId = profileDirect?.id ?: 0
                response.body()?.data?.requests?.let { dtos ->
                    val requests = dtos.map { dto ->
                        ServiceRequestEntity(
                            id = dto.id,
                            customerId = customerId,
                            title = dto.serviceType,
                            description = dto.description,
                            status = dto.status,
                            address = dto.address,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            scheduledDate = dto.scheduledDate,
                            scheduledTime = dto.scheduledTime,
                            createdAt = dto.createdAt,
                            isSynced = true
                        )
                    }
                    serviceRequestDao.insertAll(requests)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshAmcVisits() {
        try {
            val response = apiService.getAmcVisits()
            if (response.isSuccessful) {
                response.body()?.data?.let { dtos ->
                    val visits = dtos.map { dto ->
                        AmcVisit(
                            id = dto.id,
                            technicianName = dto.completedByName ?: "Pending Assignment",
                            technicianPhone = "",
                            scheduledDate = parseDate(dto.scheduledDate),
                            status = dto.status,
                            visitNumber = dto.cleaningNumber ?: 0,
                            notes = dto.visitNotes,
                            timeSlot = dto.timeSlot
                        )
                    }
                    amcVisitDao.clearAll()
                    amcVisitDao.insertAll(visits)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshInverterTelemetry(customerId: Int) {
        try {
            val response = apiService.getInverterTelemetry(customerId)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val summary = InverterGenerationSummaryEntity(
                        customerId = customerId,
                        dailyGeneration = dto.dailyGeneration,
                        totalGeneration = dto.totalGeneration,
                        peakPower = dto.peakPower,
                        currentPower = dto.currentPower,
                        isSimulated = dto.isSimulated,
                        status = dto.status,
                        lastUpdated = dto.lastUpdated
                    )
                    inverterGenerationSummaryDao.insertSummary(summary)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshInverterHistory(customerId: Int, period: String) {
        try {
            val response = apiService.getInverterHistory(customerId, period)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val points = dto.history.map { point ->
                        InverterGenerationHistoryEntity(
                            customerId = customerId,
                            period = period,
                            label = point.label,
                            powerValue = point.power,
                            generationValue = point.generation
                        )
                    }
                    inverterGenerationHistoryDao.clearHistory(customerId, period)
                    inverterGenerationHistoryDao.insertAll(points)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshInvoices(customerId: Int) {
        try {
            val response = apiService.getInvoices(customerId)
            if (response.isSuccessful) {
                response.body()?.data?.let { dtos ->
                    val invoices = dtos.map { dto ->
                        InvoiceEntity(
                            id = dto.id,
                            customerId = dto.customerId,
                            invoiceType = dto.invoiceType,
                            amount = dto.amount,
                            paymentStatus = dto.paymentStatus,
                            amountPaid = dto.amountPaid,
                            invoiceDate = dto.invoiceDate,
                            paymentDate = dto.paymentDate,
                            description = dto.description,
                            proofUrl = dto.proofUrl
                        )
                    }
                    invoiceDao.clearAll()
                    invoiceDao.insertAll(invoices)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun submitServiceRequest(
        title: String,
        description: String,
        address: String,
        latitude: Double,
        longitude: Double,
        localImage: File?
    ) {
        val customerProfileDirect = customerProfileDao.getCustomerProfileDirect()
        val customerId = customerProfileDirect?.id ?: 0

        // 1. Create a local ServiceRequestEntity in the database
        val localRequest = ServiceRequestEntity(
            id = null,
            customerId = customerId,
            title = title,
            description = description,
            address = address,
            latitude = latitude,
            longitude = longitude,
            status = "pending",
            scheduledDate = null,
            scheduledTime = null,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date()),
            isSynced = false,
            localImagePath = localImage?.absolutePath
        )
        serviceRequestDao.insertServiceRequest(localRequest)

        // 2. Enqueue WorkManager sync task with Network constraint
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val serviceSyncWorker = OneTimeWorkRequestBuilder<ServiceRequestSyncWorker>()
            .setConstraints(syncConstraints)
            .build()

        WorkManager.getInstance(context).enqueue(serviceSyncWorker)
    }

    suspend fun saveCard(card: SavedCardEntity) {
        savedCardDao.insertCard(card)
    }

    suspend fun deleteCard(card: SavedCardEntity) {
        savedCardDao.deleteCard(card)
    }

    suspend fun getActiveUserSession(): UserSessionEntity? {
        return userSessionDao.getSession()
    }

    suspend fun saveUserSession(session: UserSessionEntity) {
        userSessionDao.insertSession(session)
    }

    suspend fun clearUserSession() {
        userSessionDao.clearSession()
    }

    suspend fun getCustomerProfileDirect(): CustomerProfileEntity? {
        return customerProfileDao.getCustomerProfileDirect()
    }


    suspend fun createRazorpayOrder(amount: Double): Map<String, Any>? {
        return try {
            val response = apiService.createPaymentOrder(
                mapOf(
                    "amountInRupees" to amount,
                    "description" to "Outstanding balance payment",
                    "referenceId" to "pay_${System.currentTimeMillis()}"
                )
            )
            if (response.isSuccessful) response.body()?.data else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun verifyPayment(orderId: String, paymentId: String, signature: String): Boolean {
        return try {
            val response = apiService.verifyPayment(
                mapOf(
                    "razorpay_order_id" to orderId,
                    "razorpay_payment_id" to paymentId,
                    "razorpay_signature" to signature
                )
            )
            if (response.isSuccessful) {
                // Update local caches on success
                refreshServiceRequests()
                val profile = customerProfileDao.getCustomerProfileDirect()
                if (profile != null) {
                    refreshInvoices(profile.id)
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateFcmToken(token: String) {
        try {
            sessionManager.saveFcmToken(token)
            apiService.updateFcmToken(mapOf("fcm_token" to token))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
