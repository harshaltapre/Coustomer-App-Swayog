package com.example.coustomerapp.di

import android.content.Context
import androidx.room.Room
import com.example.coustomerapp.data.local.AppDatabase
import com.example.coustomerapp.data.local.dao.*
import com.example.coustomerapp.data.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

import com.example.coustomerapp.data.remote.AuthInterceptor
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(okHttpClient: OkHttpClient): ApiService {
        return Retrofit.Builder()
            // Set to staging URL or local computer IP
            // Emulator: http://10.0.2.2:4000, Local: http://127.0.0.1:4000, Physical: http://192.168.1.12:4000/
            .baseUrl("http://192.168.1.12:4000/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "swayog_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserSessionDao(db: AppDatabase): UserSessionDao = db.userSessionDao()

    @Provides
    fun provideCustomerProfileDao(db: AppDatabase): CustomerProfileDao = db.customerProfileDao()

    @Provides
    fun provideServiceRequestDao(db: AppDatabase): ServiceRequestDao = db.serviceRequestDao()

    @Provides
    fun provideDispatchRecordDao(db: AppDatabase): DispatchRecordDao = db.dispatchRecordDao()

    @Provides
    fun provideInverterGenerationSummaryDao(db: AppDatabase): InverterGenerationSummaryDao = db.inverterGenerationSummaryDao()

    @Provides
    fun provideInverterGenerationHistoryDao(db: AppDatabase): InverterGenerationHistoryDao = db.inverterGenerationHistoryDao()

    @Provides
    fun provideInvoiceDao(db: AppDatabase): InvoiceDao = db.invoiceDao()

    @Provides
    fun provideSavedCardDao(db: AppDatabase): SavedCardDao = db.savedCardDao()

    @Provides
    fun provideAmcVisitDao(db: AppDatabase): AmcVisitDao = db.amcVisitDao()
}
