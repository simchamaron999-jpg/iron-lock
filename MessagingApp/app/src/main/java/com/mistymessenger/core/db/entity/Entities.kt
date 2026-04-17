package com.mistymessenger.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val name: String,
    val avatarUrl: String = "",
    val bio: String = "",
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val isBlocked: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val type: String, // "dm" or "group"
    val name: String,
    val avatarUrl: String = "",
    val lastMessageText: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val inviteLink: String = "",
    val description: String = "",
    val isChatLocked: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val type: String, // text, image, video, audio, document, gif, sticker, location, contact
    val content: String = "",
    val mediaUrl: String = "",
    val mediaLocalPath: String = "",
    val mediaMimeType: String = "",
    val mediaSize: Long = 0L,
    val thumbnailUrl: String = "",
    val status: String = "sending", // sending, sent, delivered, read
    val createdAt: Long = System.currentTimeMillis(),
    val replyToId: String = "",
    val reactions: List<String> = emptyList(), // JSON string list "userId:emoji"
    val isForwarded: Boolean = false,
    val isStarred: Boolean = false,
    val isDeletedForMe: Boolean = false,
    val isDeletedForEveryone: Boolean = false,
    val deletedByOther: Boolean = false, // anti-delete: was deleted by sender
    val revokedAt: Long = 0L,
    val scheduledAt: Long = 0L,
    val linkPreviewTitle: String = "",
    val linkPreviewDescription: String = "",
    val linkPreviewImageUrl: String = "",
    val linkPreviewUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val durationMs: Long = 0L // for audio/video
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val phone: String,
    val avatarUrl: String = "",
    val customNotificationSoundUri: String = "",
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false
)

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // text, image, video
    val content: String = "",
    val mediaUrl: String = "",
    val bgColor: Long = 0L,
    val textColor: Long = 0L,
    val fontIndex: Int = 0,
    val viewers: List<String> = emptyList(),
    val expiresAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val participantIds: List<String>,
    val type: String, // voice, video
    val direction: String, // incoming, outgoing, missed
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val durationSeconds: Long = 0L
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val name: String,
    val avatarUrl: String = "",
    val authToken: String = "",
    val refreshToken: String = "",
    val isActive: Boolean = false
)

@Entity(tableName = "auto_replies")
data class AutoReplyEntity(
    @PrimaryKey val id: String,
    val trigger: String,
    val response: String,
    val scope: String = "all", // all, contact, group
    val scopeId: String = "",
    val isEnabled: Boolean = true
)

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val content: String,
    val type: String = "text",
    val mediaLocalPath: String = "",
    val scheduledAt: Long,
    val status: String = "pending" // pending, sent, cancelled
)

@Entity(tableName = "chat_settings")
data class ChatSettingsEntity(
    @PrimaryKey val chatId: String,
    val wallpaperUri: String = "",
    val wallpaperColor: Long = 0L,
    val outgoingBubbleColor: Long = 0L,
    val notificationSoundUri: String = "",
    val isMuted: Boolean = false,
    val muteUntil: Long = 0L,
    val isChatLocked: Boolean = false
)

@Entity(tableName = "starred_messages")
data class StarredMessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val starredAt: Long = System.currentTimeMillis()
)
