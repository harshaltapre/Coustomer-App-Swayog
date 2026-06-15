package com.example.coustomerapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CustomerProfileDto(
    @SerializedName("id") val id: Int,
    @SerializedName("customerCode") val customerCode: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phoneNumber") val phoneNumber: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("address") val address: String,
    @SerializedName("systemSizeKw") val systemSizeKw: Double,
    @SerializedName("installationDate") val installationDate: String?,
    @SerializedName("warrantyExpiry") val warrantyExpiry: String?,
    @SerializedName("panelBrand") val panelBrand: String?,
    @SerializedName("inverterBrand") val inverterBrand: String?,
    @SerializedName("inverterModel") val inverterModel: String?,
    @SerializedName("amcStatus") val amcStatus: String?, // 'active', 'expired', 'none'
    @SerializedName("amcExpiryDate") val amcExpiryDate: String?,
    @SerializedName("status") val liveStatus: String,
    @SerializedName("projectStage") val installationStage: Int,
    @SerializedName("cleaningsPerMonth") val cleaningsPerMonth: Int?,
    @SerializedName("completedVisits") val completedVisits: Int?,
    @SerializedName("pendingVisits") val pendingVisits: Int?,
    @SerializedName("clientType") val clientType: String?,
    @SerializedName("consumerNumber") val consumerNumber: String?,
    @SerializedName("monthlyCleaningRate") val monthlyCleaningRate: Double?,
    val treesPlanted: Int? = 0,
    val co2PreventedKg: Double? = 0.0
)

data class ServiceRequestDto(
    val id: Int,
    @SerializedName("title") val serviceType: String,
    val description: String,
    val status: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val scheduledDate: String?,
    val scheduledTime: String?,
    val createdAt: String
)

data class ServiceRequestsResponse(
    val requests: List<ServiceRequestDto>
)

data class DispatchRecordDto(
    val id: String,
    val customerId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val dispatchedAt: String,
    val notes: String?
)

data class AmcVisitDto(
    val id: String,
    val scheduledDate: String,
    val status: String,
    val visitNotes: String?,
    val cleaningNumber: Int?,
    val timeSlot: String?,
    val completedByName: String?
)

data class InverterTelemetryDto(
    val dailyGeneration: Double,
    val totalGeneration: Double,
    val peakPower: Double,
    val currentPower: Double,
    val isSimulated: Boolean,
    val status: String?,
    val lastUpdated: String?
)

data class InverterHistoryPointDto(
    val label: String,
    val power: Double?, // Used for realtime (kW)
    val generation: Double? // Used for historical (kWh)
)

data class InverterHistoryDto(
    val period: String,
    val history: List<InverterHistoryPointDto>
)

data class InvoiceDto(
    val id: String,
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
