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

// ─── Inverter generation summary ───
data class InverterGenerationResponse(
    val dailyGeneration: Double,
    val totalGeneration: Double,
    val peakPower: Double,
    val currentPower: Double,
    val isSimulated: Boolean,
    val status: String,
    val lastUpdated: String
)

// ─── Inverter history point ───
data class InverterHistoryPoint(
    val label: String,
    val power: Double?,
    val generation: Double?
)

data class InverterGenerationHistoryResponse(
    val customerId: Int,
    val period: String,
    val history: List<InverterHistoryPoint>
)

// ─── Open-Meteo Geocoding ───
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?
)

// ─── Open-Meteo Weather ───
data class WeatherResponse(
    val current: CurrentWeather?
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("wind_speed_10m") val windSpeed: Double,
    @SerializedName("weather_code")   val weatherCode: Int
)

fun translateWeatherCode(code: Int): Pair<String, String> = when (code) {
    0           -> "Sunny"        to "sunny"
    1, 2, 3     -> "Partly Cloudy" to "partly_cloudy"
    45, 48      -> "Foggy"        to "foggy"
    51, 53, 55  -> "Drizzle"      to "drizzle"
    61, 63, 65  -> "Rainy"        to "rainy"
    71, 73, 75  -> "Snowy"        to "snowy"
    80, 81, 82  -> "Rain Showers" to "rain_showers"
    95, 96, 99  -> "Thunderstorm" to "thunderstorm"
    else        -> "Cloudy"       to "cloudy"
}

// ─── Service Request ───
data class ServiceRequestBody(
    val serviceType: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val preferredDate: String
)

data class ServiceRequestResponse(
    val status: String,
    val data: ServiceRequestData?
)

data class ServiceRequestData(
    val id: Int,
    val serviceType: String,
    val status: String,
    val createdAt: String
)

// ─── Payments ───
data class CreateOrderRequest(
    val amountInRupees: Double,
    val description: String,
    val referenceId: String
)

data class CreateOrderResponse(
    val status: String,
    val data: RazorpayOrderData?
)

data class RazorpayOrderData(
    val id: String,
    val amount: Long,
    val currency: String
)

data class VerifyPaymentRequest(
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String
)
