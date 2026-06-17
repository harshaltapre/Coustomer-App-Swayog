package com.example.coustomerapp.data.remote

import com.example.coustomerapp.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: Map<String, String>): Response<ApiResponse<LoginResponse>>

    @GET("api/v1/customer/profile")
    suspend fun getProfile(): Response<ApiResponse<CustomerProfileDto>>

    @GET("api/v1/customer/stats")
    suspend fun getStats(): Response<ApiResponse<Map<String, Any>>>

    @GET("api/v1/customer/installation")
    suspend fun getInstallationTracker(): Response<ApiResponse<CustomerProfileDto>>

    @GET("api/v1/customer/dispatches")
    suspend fun getDispatches(): Response<ApiResponse<List<DispatchRecordDto>>>

    @GET("api/v1/customer/requests")
    suspend fun getServiceRequests(@Query("status") status: String? = null): Response<ApiResponse<ServiceRequestsResponse>>

    @Multipart
    @POST("api/v1/customer/requests")
    suspend fun createServiceRequest(
        @Part("serviceType") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("address") address: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("preferredDate") preferredDate: RequestBody,
        @Part images: List<MultipartBody.Part>? = null
    ): Response<ApiResponse<ServiceRequestDto>>

    @POST("api/v1/customer/payments/razorpay/order")
    suspend fun createPaymentOrder(@Body request: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>

    @POST("api/v1/customer/payments/razorpay/verify")
    suspend fun verifyPayment(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    @GET("api/v1/customer/amc-visits")
    suspend fun getAmcVisits(): Response<ApiResponse<List<AmcVisitDto>>>

    @POST("api/v1/customer/fcm-token")
    suspend fun updateFcmToken(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    // Telemetry and Invoices APIs

    @GET("api/v1/subadmin/customers/{customerId}/inverter-generation")
    suspend fun getInverterTelemetry(
        @Path("customerId") customerId: Int
    ): Response<InverterTelemetryDto>

    @GET("api/v1/subadmin/customers/{customerId}/inverter-generation-history")
    suspend fun getInverterHistory(
        @Path("customerId") customerId: Int,
        @Query("period") period: String
    ): Response<InverterHistoryDto>

    @GET("api/v1/invoices")
    suspend fun getInvoices(
        @Query("customerId") customerId: Int
    ): Response<ApiResponse<List<InvoiceDto>>>

    // ─── Telemetry & Weather (Master Prompt v2.0) ───
    @GET("api/v1/subadmin/customers/{customerId}/inverter-generation")
    suspend fun getInverterGenerationSummary(
        @Path("customerId") customerId: Int
    ): Response<InverterGenerationResponse>

    @GET("api/v1/subadmin/customers/{customerId}/inverter-generation-history")
    suspend fun getInverterGenerationHistory(
        @Path("customerId") customerId: Int,
        @Query("period") period: String
    ): Response<InverterGenerationHistoryResponse>

    @Multipart
    @POST("api/v1/customer/requests")
    suspend fun createServiceRequestNew(
        @Part("serviceType") serviceType: RequestBody,
        @Part("description")  description: RequestBody,
        @Part("address")      address: RequestBody,
        @Part("latitude")     latitude: RequestBody,
        @Part("longitude")    longitude: RequestBody,
        @Part("preferredDate") preferredDate: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Response<ServiceRequestResponse>

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun getCoordinatesByCity(
        @Query("name")     cityName: String,
        @Query("count")    count: Int = 1,
        @Query("language") language: String = "en",
        @Query("format")   format: String = "json"
    ): Response<GeocodingResponse>

    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getWeather(
        @Query("latitude")  latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current")   current: String = "temperature_2m,wind_speed_10m,weather_code"
    ): Response<WeatherResponse>
}
