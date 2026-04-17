package com.mistymessenger.status.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.StatusDao
import com.mistymessenger.core.db.entity.StatusEntity
import com.mistymessenger.core.media.S3UploadService
import com.mistymessenger.core.network.RetrofitClient
import com.mistymessenger.core.network.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.InputStream
import java.net.URL
import javax.inject.Inject

@Serializable
data class PostStatusRequest(
    val type: String,
    val content: String,
    val mediaUrl: String = "",
    val bgColor: String = "#000000",
    val textColor: String = "#FFFFFF",
    val fontIndex: Int = 0
)

@Serializable
data class StatusResponse(
    val _id: String,
    val user: StatusUserDto,
    val type: String,
    val content: String,
    val mediaUrl: String = "",
    val bgColor: String = "#000000",
    val textColor: String = "#FFFFFF",
    val fontIndex: Int = 0,
    val expiresAt: String,
    val createdAt: String
)

@Serializable
data class StatusUserDto(val _id: String, val name: String, val avatarUrl: String = "")

interface StatusApiService {
    @GET("statuses") suspend fun getStatuses(): List<StatusResponse>
    @POST("statuses") suspend fun postStatus(@Body req: PostStatusRequest): StatusResponse
    @POST("statuses/{id}/view") suspend fun markViewed(@Path("id") id: String): Unit
    @DELETE("statuses/{id}") suspend fun deleteStatus(@Path("id") id: String): Unit
}

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusDao: StatusDao,
    private val tokenProvider: TokenProvider,
    private val uploadService: S3UploadService,
    retrofitClient: RetrofitClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val api = retrofitClient.retrofit.create(StatusApiService::class.java)

    val myName: String get() = tokenProvider.getUserName()
    val myAvatarUrl: String get() = ""

    val myStatuses: StateFlow<List<StatusEntity>> = statusDao.getActiveStatuses()
        .map { it.filter { s -> s.userId == tokenProvider.getUserId() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactStatuses: StateFlow<List<Pair<String, List<StatusEntity>>>> = statusDao.getActiveStatuses()
        .map { all ->
            val myId = tokenProvider.getUserId()
            all.filter { it.userId != myId }
                .groupBy { it.userId }
                .map { (uid, statuses) -> uid to statuses.sortedBy { it.createdAt } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isPosting = MutableStateFlow(false)
    val isPosting = _isPosting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init { syncStatuses() }

    private fun syncStatuses() {
        viewModelScope.launch {
            runCatching {
                val remote = api.getStatuses()
                statusDao.insertAll(remote.map { it.toEntity() })
            }
        }
    }

    fun getStatusesForUser(userId: String) = statusDao.getStatusesForUser(userId)

    fun postTextStatus(text: String, bgColor: Long, textColor: Long, fontIndex: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            _isPosting.value = true
            runCatching {
                val resp = api.postStatus(
                    PostStatusRequest(
                        type = "text",
                        content = text,
                        bgColor = "#%06X".format(bgColor and 0xFFFFFF),
                        textColor = "#%06X".format(textColor and 0xFFFFFF),
                        fontIndex = fontIndex
                    )
                )
                statusDao.insert(resp.toEntity())
            }.onFailure { _error.value = it.message }
            _isPosting.value = false
            onDone()
        }
    }

    fun postMediaStatus(uri: Uri, mimeType: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isPosting.value = true
            runCatching {
                uploadService.uploadUri(uri, mimeType, compress = false).collect { state ->
                    if (state is com.mistymessenger.core.media.UploadState.Success) {
                        val type = if (mimeType.startsWith("video")) "video" else "image"
                        val resp = api.postStatus(PostStatusRequest(type = type, content = "", mediaUrl = state.result.url))
                        statusDao.insert(resp.toEntity())
                    }
                }
            }.onFailure { _error.value = it.message }
            _isPosting.value = false
            onDone()
        }
    }

    fun markViewed(statusId: String) {
        viewModelScope.launch { runCatching { api.markViewed(statusId) } }
    }

    fun downloadStatus(status: StatusEntity) {
        viewModelScope.launch {
            runCatching {
                val url = status.mediaUrl.ifBlank { return@runCatching }
                val isVideo = status.type == "video"
                val fileName = "MistyStatus_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH,
                            if (isVideo) "Movies/MistyMessenger" else "Pictures/MistyMessenger")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                 else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = context.contentResolver.insert(collection, contentValues) ?: return@runCatching
                val inputStream: InputStream = URL(url).openStream()
                context.contentResolver.openOutputStream(uri)?.use { out -> inputStream.copyTo(out) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
            }
        }
    }

    fun deleteMyStatus(statusId: String) {
        viewModelScope.launch {
            runCatching {
                api.deleteStatus(statusId)
                statusDao.deleteById(statusId)
            }
        }
    }

    private fun StatusResponse.toEntity() = StatusEntity(
        id = _id,
        userId = user._id,
        type = type,
        content = content,
        mediaUrl = mediaUrl,
        bgColor = parseHexColor(bgColor),
        textColor = parseHexColor(textColor),
        fontIndex = fontIndex,
        expiresAt = try { java.time.Instant.parse(expiresAt).toEpochMilli() } catch (e: Exception) { System.currentTimeMillis() + 86400_000L },
        createdAt = try { java.time.Instant.parse(createdAt).toEpochMilli() } catch (e: Exception) { System.currentTimeMillis() }
    )

    private fun parseHexColor(hex: String): Long {
        return try { (java.lang.Long.parseLong(hex.trimStart('#'), 16) or 0xFF000000L) } catch (e: Exception) { 0xFF000000L }
    }
}
