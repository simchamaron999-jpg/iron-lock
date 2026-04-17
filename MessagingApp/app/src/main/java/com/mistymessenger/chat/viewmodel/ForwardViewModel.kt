package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.core.network.RetrofitClient
import com.mistymessenger.core.network.SocketManager
import com.mistymessenger.core.network.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ForwardViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val socketManager: SocketManager,
    private val tokenProvider: TokenProvider,
    retrofitClient: RetrofitClient
) : ViewModel() {

    private val api = retrofitClient.retrofit.create(ChatApiService::class.java)

    private val _query = MutableStateFlow("")
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _contactSelections = MutableStateFlow<Set<String>>(emptySet()) // userId → need to create/get DM
    private var targetMessage: MessageEntity? = null
    val selectedIds = _selectedIds.asStateFlow()

    val chats: StateFlow<List<ChatEntity>> = _query
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) chatDao.getActiveChats()
            else chatDao.searchChats(q).let { flowOf(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = _query
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else contactDao.searchContacts(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadMessage(messageId: String) {
        viewModelScope.launch {
            targetMessage = messageDao.getById(messageId)
        }
    }

    fun onQueryChange(q: String) { _query.value = q }

    fun toggleSelection(chatId: String) {
        _selectedIds.update { current ->
            if (chatId in current) current - chatId else current + chatId
        }
    }

    fun toggleContactSelection(userId: String) {
        _contactSelections.update { current ->
            if (userId in current) current - userId else current + userId
        }
        // Add userId to selectedIds as a proxy; resolved to chatId on forward
        _selectedIds.update { it + userId }
    }

    fun forwardMessage(onDone: () -> Unit) {
        val msg = targetMessage ?: return
        viewModelScope.launch {
            // Forward to direct chat IDs
            _selectedIds.value.forEach { id ->
                val chatId = if (_contactSelections.value.contains(id)) {
                    // Need to resolve userId → chatId
                    runCatching { api.createOrGetDm(CreateDmRequest(id)).id }.getOrNull() ?: return@forEach
                } else id

                val newId = UUID.randomUUID().toString()
                socketManager.emit("message:send", JSONObject().apply {
                    put("id", newId)
                    put("chatId", chatId)
                    put("type", msg.type)
                    put("content", msg.content)
                    put("mediaUrl", msg.mediaUrl)
                    put("isForwarded", true)
                })
            }
            onDone()
        }
    }
}
