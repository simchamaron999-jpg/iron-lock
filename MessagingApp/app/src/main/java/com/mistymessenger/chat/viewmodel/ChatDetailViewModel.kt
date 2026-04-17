package com.mistymessenger.chat.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.dao.StarredMessageDao
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.core.db.entity.StarredMessageEntity
import com.mistymessenger.core.network.SocketManager
import com.mistymessenger.core.network.TokenProvider
import com.mistymessenger.chat.repository.ChatSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

sealed class ChatEvent {
    data class ShowContextMenu(val message: MessageEntity) : ChatEvent()
    data class ShowEmojiSheet(val message: MessageEntity) : ChatEvent()
    data class ShowDeleteDialog(val message: MessageEntity) : ChatEvent()
    data class ShowForward(val messageId: String) : ChatEvent()
    object None : ChatEvent()
}

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val starredMessageDao: StarredMessageDao,
    private val socketManager: SocketManager,
    private val tokenProvider: TokenProvider,
    private val chatSyncRepository: ChatSyncRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val currentUserId: String get() = tokenProvider.getUserId()

    private val _chatId = MutableStateFlow("")
    val chat: StateFlow<ChatEntity?> = _chatId
        .flatMapLatest { chatDao.getChatById(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val messages = _chatId
        .flatMapLatest { chatId ->
            Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
                messageDao.getMessagesForChat(chatId)
            }.flow
        }
        .cachedIn(viewModelScope)

    private val _typingUsers = MutableStateFlow<Set<String>>(emptySet())
    val typingUsers = _typingUsers.asStateFlow()

    private val _replyTarget = MutableStateFlow<MessageEntity?>(null)
    val replyTarget = _replyTarget.asStateFlow()

    private val _event = MutableStateFlow<ChatEvent>(ChatEvent.None)
    val event = _event.asStateFlow()

    private var typingJob: Job? = null

    fun init(chatId: String) {
        _chatId.value = chatId
        subscribeToSocket(chatId)
        markAsRead(chatId)
    }

    private fun subscribeToSocket(chatId: String) {
        viewModelScope.launch {
            socketManager.listenFlow("message:receive").collect { json ->
                if (json.optString("chatId") == chatId) {
                    val entity = json.toMessageEntity(chatId)
                    messageDao.insert(entity)
                    chatDao.updateLastMessage(chatId, entity.content, entity.createdAt)
                    // Emit delivered receipt back
                    socketManager.emit("message:delivered", JSONObject().apply {
                        put("messageId", entity.id)
                        put("chatId", chatId)
                    })
                }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("message:status").collect { json ->
                messageDao.updateStatus(json.getString("messageId"), json.getString("status"))
            }
        }
        // chat:read_receipt — our sent messages were read by recipient
        viewModelScope.launch {
            socketManager.listenFlow("chat:read_receipt").collect { json ->
                if (json.optString("chatId") == chatId) {
                    // Mark all our sent messages as read in Room
                    // (Full implementation iterates visible messages; simplified here)
                }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("message:deleted").collect { json ->
                val msgId = json.getString("messageId")
                when (json.getString("deletedFor")) {
                    "everyone" -> messageDao.markDeletedByOther(msgId)
                    "me" -> messageDao.deleteForMe(msgId)
                }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("message:reactions_updated").collect { json ->
                val msgId = json.getString("messageId")
                val reactionsArray = json.getJSONArray("reactions")
                val reactions = (0 until reactionsArray.length()).map { i ->
                    val obj = reactionsArray.getJSONObject(i)
                    "${obj.getString("userId")}:${obj.getString("emoji")}"
                }
                messageDao.updateReactions(msgId, reactions)
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("typing:start").collect { json ->
                if (json.optString("chatId") == chatId) {
                    _typingUsers.update { it + json.getString("userId") }
                }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("typing:stop").collect { json ->
                if (json.optString("chatId") == chatId) {
                    _typingUsers.update { it - json.getString("userId") }
                }
            }
        }
    }

    // ── Sending ───────────────────────────────────────────────────────────────

    fun sendTextMessage(content: String) {
        val messageId = UUID.randomUUID().toString()
        val replyTo = _replyTarget.value?.id ?: ""
        val entity = MessageEntity(
            id = messageId,
            chatId = _chatId.value,
            senderId = currentUserId,
            type = "text",
            content = content,
            status = "sending",
            createdAt = System.currentTimeMillis(),
            replyToId = replyTo
        )
        viewModelScope.launch {
            messageDao.insert(entity)
            chatDao.updateLastMessage(_chatId.value, content, entity.createdAt)
            socketManager.emit("message:send", JSONObject().apply {
                put("id", messageId)
                put("chatId", _chatId.value)
                put("type", "text")
                put("content", content)
                put("replyToId", replyTo)
            })
        }
        clearReply()
    }

    // ── Typing ────────────────────────────────────────────────────────────────

    fun onTyping() {
        socketManager.emit("typing:start", JSONObject().apply { put("chatId", _chatId.value) })
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            socketManager.emit("typing:stop", JSONObject().apply { put("chatId", _chatId.value) })
        }
    }

    // ── Reactions ─────────────────────────────────────────────────────────────

    fun sendReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            socketManager.emit("message:react", JSONObject().apply {
                put("messageId", messageId)
                put("emoji", emoji)
                put("chatId", _chatId.value)
            })
        }
    }

    // ── Star / Unstar ─────────────────────────────────────────────────────────

    fun toggleStar(message: MessageEntity) {
        viewModelScope.launch {
            val newState = !message.isStarred
            messageDao.setStarred(message.id, newState)
            if (newState) {
                starredMessageDao.insert(StarredMessageEntity(messageId = message.id, chatId = message.chatId))
            } else {
                starredMessageDao.delete(message.id)
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteForMe(messageId: String) {
        viewModelScope.launch {
            messageDao.deleteForMe(messageId)
            socketManager.emit("message:delete", JSONObject().apply {
                put("messageId", messageId)
                put("deleteFor", "me")
            })
        }
    }

    fun deleteForEveryone(messageId: String) {
        viewModelScope.launch {
            socketManager.emit("message:delete", JSONObject().apply {
                put("messageId", messageId)
                put("deleteFor", "everyone")
            })
            // Optimistically update locally; socket event will confirm
            messageDao.deleteForEveryone(messageId)
        }
    }

    // ── Copy ──────────────────────────────────────────────────────────────────

    fun copyMessage(content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("message", content))
    }

    fun copyAllMessages(chatId: String) {
        viewModelScope.launch {
            val msgs = messageDao.searchMessages("") // fetches recent; full impl iterates all
            val text = msgs.joinToString("\n") { m ->
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(m.createdAt))
                "[$time] ${m.content}"
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("chat", text))
        }
    }

    // ── Reply ─────────────────────────────────────────────────────────────────

    fun setReplyTarget(message: MessageEntity) { _replyTarget.value = message }
    fun clearReply() { _replyTarget.value = null }

    // ── Events (bottom sheet triggers) ───────────────────────────────────────

    fun onMessageLongPress(message: MessageEntity) {
        _event.value = ChatEvent.ShowContextMenu(message)
    }

    fun showEmojiSheet(message: MessageEntity) {
        _event.value = ChatEvent.ShowEmojiSheet(message)
    }

    fun showDeleteDialog(message: MessageEntity) {
        _event.value = ChatEvent.ShowDeleteDialog(message)
    }

    fun clearEvent() { _event.value = ChatEvent.None }

    // ── Read ──────────────────────────────────────────────────────────────────

    private fun markAsRead(chatId: String) {
        viewModelScope.launch {
            chatDao.clearUnread(chatId)
            socketManager.emit("chat:read", JSONObject().apply { put("chatId", chatId) })
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun JSONObject.toMessageEntity(chatId: String) = MessageEntity(
        id = optString("id", UUID.randomUUID().toString()),
        chatId = chatId,
        senderId = optString("senderId"),
        type = optString("type", "text"),
        content = optString("content"),
        mediaUrl = optString("mediaUrl"),
        thumbnailUrl = optString("thumbnailUrl"),
        status = "delivered",
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        replyToId = optString("replyToId"),
        isForwarded = optBoolean("isForwarded", false),
        linkPreviewTitle = optJSONObject("linkPreview")?.optString("title") ?: "",
        linkPreviewDescription = optJSONObject("linkPreview")?.optString("description") ?: "",
        linkPreviewImageUrl = optJSONObject("linkPreview")?.optString("imageUrl") ?: "",
        linkPreviewUrl = optJSONObject("linkPreview")?.optString("url") ?: ""
    )
}
