package com.example.coustomerapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import androidx.work.*
import com.example.coustomerapp.data.local.dao.*
import com.example.coustomerapp.data.local.entities.*
import com.example.coustomerapp.data.remote.ApiService
import com.example.coustomerapp.data.remote.dto.*
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
    private val inverterDao: InverterDao,
    private val sessionManager: SessionManager
) {
    val customerProfile: Flow<CustomerProfileEntity?> = customerProfileDao.getCustomerProfile()
    val dispatches: Flow<List<DispatchRecordEntity>> = dispatchRecordDao.getAllDispatches()
    fun searchDispatches(query: String): Flow<List<DispatchRecordEntity>> = dispatchRecordDao.searchDispatches(query)

    val serviceRequests: Flow<List<ServiceRequestEntity>> = serviceRequestDao.getAllRequests()
    val amcVisits: Flow<List<AmcVisit>> = amcVisitDao.getAllAmcVisits()
    val savedCards: Flow<List<SavedCardEntity>> = savedCardDao.getSavedCards()

    fun getInvoices(customerId: Int): Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices(customerId)

    fun getInverterSummary(customerId: Int): Flow<InverterGenerationSummaryEntity?> =
        inverterDao.getSummary(customerId)

    fun getInverterHistory(customerId: Int, period: String): Flow<List<InverterGenerationHistoryEntity>> =
        inverterDao.getHistory(customerId, period)

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
                            serverId = dto.id,
                            serviceType = dto.serviceType,
                            description = dto.description,
                            address = dto.address ?: "",
                            latitude = dto.latitude ?: 0.0,
                            longitude = dto.longitude ?: 0.0,
                            preferredDate = dto.scheduledDate ?: "",
                            status = dto.status,
                            isSynced = true,
                            createdAt = dto.createdAt
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

    // ─── Inverter: Summary ───
    suspend fun refreshInverterTelemetry(customerId: Int) {
        try {
            val response = apiService.getInverterGenerationSummary(customerId)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    inverterDao.insertSummary(
                        InverterGenerationSummaryEntity(
                            customerId    = customerId,
                            dailyGeneration = body.dailyGeneration,
                            totalGeneration = body.totalGeneration,
                            peakPower     = body.peakPower,
                            currentPower  = body.currentPower,
                            isSimulated   = body.isSimulated,
                            status        = body.status,
                            lastUpdated   = body.lastUpdated
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "refreshInverterTelemetry error: ${e.message}")
        }
    }

    // ─── Inverter: History ───
    suspend fun refreshInverterHistory(customerId: Int, period: String) {
        try {
            val response = apiService.getInverterGenerationHistory(customerId, period)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    inverterDao.clearHistory(customerId, period)
                    val entities = body.history.map { point ->
                        InverterGenerationHistoryEntity(
                            customerId      = customerId,
                            period          = period,
                            label           = point.label,
                            powerValue      = point.power,
                            generationValue = point.generation
                        )
                    }
                    inverterDao.insertHistory(entities)
                }
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "refreshInverterHistory error: ${e.message}")
        }
    }

    // ─── Weather ───
    suspend fun fetchWeatherForCity(cityName: String): CurrentWeather? {
        return try {
            val geoResponse = apiService.getCoordinatesByCity(cityName)
            if (geoResponse.isSuccessful) {
                val result = geoResponse.body()?.results?.firstOrNull()
                if (result != null) {
                    val weatherRes = apiService.getWeather(result.latitude, result.longitude)
                    if (weatherRes.isSuccessful) return weatherRes.body()?.current
                }
            }
            null
        } catch (e: Exception) {
            Log.e("CustomerRepository", "fetchWeather error: ${e.message}")
            null
        }
    }

    // ─── Service Requests ───
    suspend fun saveServiceRequestLocally(entity: ServiceRequestEntity): Long =
        serviceRequestDao.insert(entity)

    fun getAllServiceRequests(): Flow<List<ServiceRequestEntity>> =
        serviceRequestDao.getAllRequests()

    suspend fun getUnsyncedRequests(): List<ServiceRequestEntity> =
        serviceRequestDao.getUnsyncedRequests()

    suspend fun markRequestSynced(localId: Int, serverId: Int) =
        serviceRequestDao.markSynced(localId, serverId)

    suspend fun uploadServiceRequest(entity: ServiceRequestEntity, imageFile: File?): Boolean {
        return try {
            val imagePart = imageFile?.let {
                val compressed = compressImageBelow500KB(it)
                val requestFile = compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images", compressed.name, requestFile)
            }
            val response = apiService.createServiceRequestNew(
                serviceType  = entity.serviceType.toRequestBody("text/plain".toMediaTypeOrNull()),
                description  = entity.description.toRequestBody("text/plain".toMediaTypeOrNull()),
                address      = entity.address.toRequestBody("text/plain".toMediaTypeOrNull()),
                latitude     = entity.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                longitude    = entity.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                preferredDate = entity.preferredDate.toRequestBody("text/plain".toMediaTypeOrNull()),
                image        = imagePart
            )
            if (response.isSuccessful) {
                response.body()?.data?.id?.let { serverId ->
                    serviceRequestDao.markSynced(entity.localId, serverId)
                }
                true
            } else false
        } catch (e: Exception) {
            Log.e("CustomerRepository", "uploadServiceRequest error: ${e.message}")
            false
        }
    }

    // ─── Image compression helper ───
    private fun compressImageBelow500KB(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return file
        val outputFile = File(file.parent, "compressed_${file.name}")
        var quality = 90
        do {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            outputFile.writeBytes(stream.toByteArray())
            quality -= 10
        } while (outputFile.length() > 500_000 && quality > 10)
        return outputFile
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
