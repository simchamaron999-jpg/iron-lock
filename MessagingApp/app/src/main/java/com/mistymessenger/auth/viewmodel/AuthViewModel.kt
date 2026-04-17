package com.mistymessenger.auth.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.auth.repository.AuthRepository
import com.mistymessenger.core.network.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import javax.inject.Inject

data class AuthUiState(val isLoading: Boolean = false, val error: String? = null)

@Serializable
data class MediaUploadResponse(val url: String)

interface MediaApiService {
    @Multipart
    @POST("media/upload")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): MediaUploadResponse
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val retrofitClient: RetrofitClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun sendOTP(phone: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.sendOTP(phone)
                .onSuccess { onSent() }
                .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun verifyOTP(phone: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.verifyOTP(phone, otp)
                .onSuccess { onSuccess() }
                .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun saveProfile(name: String, bio: String, avatarUri: Uri?, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            var avatarUrl = ""
            if (avatarUri != null) {
                runCatching {
                    val api = retrofitClient.retrofit.create(MediaApiService::class.java)
                    val file = uriToFile(avatarUri)
                    val part = MultipartBody.Part.createFormData(
                        "file", file.name,
                        file.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    api.uploadAvatar(part).url
                }.onSuccess { url -> avatarUrl = url }
            }

            repository.saveProfile(name, bio, avatarUrl)
                .onSuccess { onDone() }
                .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun uriToFile(uri: Uri): File {
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open uri")
        val temp = File.createTempFile("avatar_", ".jpg", context.cacheDir)
        temp.outputStream().use { input.copyTo(it) }
        return temp
    }
}
