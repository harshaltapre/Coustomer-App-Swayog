package com.example.coustomerapp.data.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.coustomerapp.data.local.dao.ServiceRequestDao
import com.example.coustomerapp.data.remote.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class ServiceRequestSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val serviceRequestDao: ServiceRequestDao,
    private val apiService: ApiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val unsynced = serviceRequestDao.getUnsyncedRequests()
            if (unsynced.isEmpty()) {
                return Result.success()
            }

            for (request in unsynced) {
                val titlePart = request.title.toRequestBody("text/plain".toMediaTypeOrNull())
                val descPart = request.description.toRequestBody("text/plain".toMediaTypeOrNull())
                val addressPart = (request.address ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                val latPart = (request.latitude ?: 0.0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val lonPart = (request.longitude ?: 0.0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val datePart = (request.scheduledDate ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

                val imageParts = mutableListOf<MultipartBody.Part>()
                if (!request.localImagePath.isNullOrBlank()) {
                    val compressedFile = compressImageFile(context, request.localImagePath)
                    if (compressedFile != null && compressedFile.exists()) {
                        val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        imageParts.add(MultipartBody.Part.createFormData("images", compressedFile.name, requestFile))
                    }
                }

                val response = apiService.createServiceRequest(
                    title = titlePart,
                    description = descPart,
                    address = addressPart,
                    latitude = latPart,
                    longitude = lonPart,
                    preferredDate = datePart,
                    images = if (imageParts.isNotEmpty()) imageParts else null
                )

                if (response.isSuccessful && response.body() != null) {
                    val remoteId = response.body()!!.data.id
                    serviceRequestDao.markAsSynced(request.localId, remoteId)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun compressImageFile(context: Context, path: String): File? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            val originalBitmap = BitmapFactory.decodeFile(path) ?: return null

            // Limit dimensions to 1280px max bounds
            val maxDimension = 1280
            val width = originalBitmap.width
            val height = originalBitmap.height
            if (width <= 0 || height <= 0) return null
            val ratio = width.toFloat() / height.toFloat()

            val (newWidth, newHeight) = if (width > height) {
                Pair(maxDimension, (maxDimension / ratio).toInt())
            } else {
                Pair((maxDimension * ratio).toInt(), maxDimension)
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            val compressedFile = File(context.cacheDir, "scaled_${file.name}")
            val outputStream = FileOutputStream(compressedFile)

            // Compress with JPEG at 75% quality
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            outputStream.flush()
            outputStream.close()

            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
