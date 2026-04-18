package com.mistymessenger.core.media

import android.content.Context
import android.net.Uri
import com.mistymessenger.core.network.RetrofitClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

data class UploadResult(val url: String, val mimeType: String, val sizeBytes: Long)

sealed class UploadState {
    data class Progress(val percent: Int) : UploadState()
    data class Success(val result: UploadResult) : UploadState()
    data class Error(val message: String) : UploadState()
}

interface MediaApiService {
    @Multipart
    @POST("media/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Query("compress") compress: Boolean = true
    ): UploadResponse

    @Multipart
    @POST("media/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UploadResponse
}

data class UploadResponse(val url: String, val mimeType: String, val sizeBytes: Long)

@Singleton
class S3UploadService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient
) {
    private val api by lazy { retrofitClient.retrofit.create(MediaApiService::class.java) }

    fun uploadUri(
        uri: Uri,
        mimeType: String,
        compress: Boolean = true
    ): Flow<UploadState> = flow {
        emit(UploadState.Progress(0))
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: run { emit(UploadState.Error("Cannot read file")); return@flow }
        val size = bytes.size.toLong()
        emit(UploadState.Progress(50))

        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val fileName = uri.lastPathSegment ?: "upload"
        val part = MultipartBody.Part.createFormData("file", fileName, body)

        runCatching { api.uploadFile(part, compress) }
            .onSuccess { resp ->
                emit(UploadState.Progress(100))
                emit(UploadState.Success(UploadResult(resp.url, resp.mimeType, size)))
            }
            .onFailure { emit(UploadState.Error(it.message ?: "Upload failed")) }
    }.flowOn(Dispatchers.IO)
}
