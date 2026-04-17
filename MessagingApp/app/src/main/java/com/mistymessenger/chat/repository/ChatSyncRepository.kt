package com.mistymessenger.chat.repository

import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.core.network.RetrofitClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

// ── API models ────────────────────────────────────────────────────────────────

@Serializable
data class ChatApiModel(
    @SerialName("_id") val id: String,
    val type: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val members: List<MemberApiModel> = emptyList(),
    val lastMessage: MessageApiModel? = null,
    val updatedAt: String = ""
)

@Serializable
data class MemberApiModel(
    @SerialName("_id") val id: String,
    val name: String = "",
    val avatarUrl: String = "",
    val isOnline: Boolean = false,
    val phone: String = ""
)

@Serializable
data class MessageApiModel(
    @SerialName("_id") val id: String,
    val chat: String = "",
    val sender: SenderApiModel? = null,
    val type: String = "text",
    val content: String = "",
    val mediaUrl: String? = null,
    val mediaMimeType: String? = null,
    val mediaSize: Long? = null,
    val thumbnailUrl: String? = null,
    val status: String = "sent",
    val createdAt: String = "",
    val replyTo: MessageApiModel? = null,
    val reactions: List<ReactionApiModel> = emptyList(),
    val isForwarded: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val linkPreview: LinkPreviewApiModel? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val duration: Long? = null
)

@Serializable data class SenderApiModel(@SerialName("_id") val id: String, val name: String = "", val avatarUrl: String = "")
@Serializable data class ReactionApiModel(val userId: String, val emoji: String)
@Serializable data class LinkPreviewApiModel(val title: String = "", val description: String = "", val imageUrl: String = "", val url: String = "")

interface ChatApiService {
    @GET("chats") suspend fun getChats(): List<ChatApiModel>
    @GET("messages/{chatId}") suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30
    ): List<MessageApiModel>
}

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class ChatSyncRepository @Inject constructor(
    retrofitClient: RetrofitClient,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    private val api = retrofitClient.retrofit.create(ChatApiService::class.java)

    suspend fun syncChats(myUserId: String) {
        runCatching {
            val remoteChats = api.getChats()
            val entities = remoteChats.map { it.toEntity(myUserId) }
            chatDao.insertAll(entities)

            // Sync last 30 messages for each chat so the UI is populated immediately
            remoteChats.forEach { chat ->
                runCatching {
                    val messages = api.getMessages(chat.id, limit = 30)
                    messageDao.insertAll(messages.map { it.toEntity(chat.id) })
                }
            }
        }
    }

    suspend fun loadOlderMessages(chatId: String, cursor: String): List<MessageEntity> {
        return runCatching {
            val remote = api.getMessages(chatId, cursor = cursor, limit = 30)
            val entities = remote.map { it.toEntity(chatId) }
            messageDao.insertAll(entities)
            entities
        }.getOrDefault(emptyList())
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

fun ChatApiModel.toEntity(myUserId: String): ChatEntity {
    val otherMember = members.firstOrNull { it.id != myUserId }
    return ChatEntity(
        id = id,
        type = type,
        name = if (type == "group") (name ?: "Group") else (otherMember?.name ?: "Unknown"),
        avatarUrl = if (type == "group") (avatarUrl ?: "") else (otherMember?.avatarUrl ?: ""),
        lastMessageText = lastMessage?.let { formatPreview(it) } ?: "",
        lastMessageAt = parseIsoMs(updatedAt),
        memberIds = members.map { it.id }
    )
}

fun MessageApiModel.toEntity(chatId: String): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    senderId = sender?.id ?: "",
    type = type,
    content = if (deletedForEveryone) "" else content,
    mediaUrl = mediaUrl ?: "",
    mediaMimeType = mediaMimeType ?: "",
    mediaSize = mediaSize ?: 0L,
    thumbnailUrl = thumbnailUrl ?: "",
    status = status,
    createdAt = parseIsoMs(createdAt),
    replyToId = replyTo?.id ?: "",
    reactions = reactions.map { "${it.userId}:${it.emoji}" },
    isForwarded = isForwarded,
    isDeletedForEveryone = deletedForEveryone,
    linkPreviewTitle = linkPreview?.title ?: "",
    linkPreviewDescription = linkPreview?.description ?: "",
    linkPreviewImageUrl = linkPreview?.imageUrl ?: "",
    linkPreviewUrl = linkPreview?.url ?: "",
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
    durationMs = duration ?: 0L
)

private fun formatPreview(msg: MessageApiModel): String = when {
    msg.deletedForEveryone -> "This message was deleted"
    msg.type == "text" -> msg.content
    msg.type == "image" -> "Photo"
    msg.type == "video" -> "Video"
    msg.type == "audio" -> "Voice message"
    msg.type == "document" -> "Document"
    msg.type == "location" -> "Location"
    else -> msg.content
}

private fun parseIsoMs(iso: String): Long {
    if (iso.isBlank()) return System.currentTimeMillis()
    return try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            .parse(iso.substringBefore("."))?.time ?: System.currentTimeMillis()
    } catch (e: Exception) { System.currentTimeMillis() }
}
