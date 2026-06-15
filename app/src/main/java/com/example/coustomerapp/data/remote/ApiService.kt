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
}
