package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mistymessenger.chat.repository.ChatRepository
import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.core.network.SocketManager
import com.mistymessenger.core.network.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val socketManager: SocketManager,
    private val tokenProvider: TokenProvider,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val currentUserId: String get() = tokenProvider.getUserId()

    private val _chatId = MutableStateFlow("")
    val chat: StateFlow<ChatEntity?> = _chatId
        .flatMapLatest { chatDao.getChatById(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val messages = _chatId
        .flatMapLatest { chatId ->
            Pager(PagingConfig(pageSize = 30)) { messageDao.getMessagesForChat(chatId) }.flow
        }
        .cachedIn(viewModelScope)

    private val _typingUsers = MutableStateFlow<Set<String>>(emptySet())
    val typingUsers = _typingUsers.asStateFlow()

    private val _replyTarget = MutableStateFlow<MessageEntity?>(null)
    val replyTarget = _replyTarget.asStateFlow()

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
                    val entity = json.toMessageEntity()
                    messageDao.insert(entity)
                    chatDao.updateLastMessage(chatId, entity.content, entity.createdAt)
                }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("message:status").collect { json ->
                messageDao.updateStatus(json.getString("messageId"), json.getString("status"))
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("message:deleted").collect { json ->
                val msgId = json.getString("messageId")
                val deletedFor = json.getString("deletedFor")
                if (deletedFor == "everyone") {
                    // Anti-delete: keep content, mark as deletedByOther
                    messageDao.markDeletedByOther(msgId)
                }
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

    fun onTyping() {
        socketManager.emit("typing:start", JSONObject().apply { put("chatId", _chatId.value) })
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            socketManager.emit("typing:stop", JSONObject().apply { put("chatId", _chatId.value) })
        }
    }

    fun setReplyTarget(message: MessageEntity) { _replyTarget.value = message }
    fun clearReply() { _replyTarget.value = null }

    fun onMessageLongPress(message: MessageEntity) {
        // Handled by UI — show context menu with react/reply/forward/delete/star
    }

    private fun markAsRead(chatId: String) {
        viewModelScope.launch {
            chatDao.clearUnread(chatId)
            socketManager.emit("chat:read", JSONObject().apply { put("chatId", chatId) })
        }
    }

    private fun JSONObject.toMessageEntity() = MessageEntity(
        id = optString("id", UUID.randomUUID().toString()),
        chatId = optString("chatId"),
        senderId = optString("senderId"),
        type = optString("type", "text"),
        content = optString("content"),
        mediaUrl = optString("mediaUrl"),
        status = "delivered",
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        replyToId = optString("replyToId")
    )
}
