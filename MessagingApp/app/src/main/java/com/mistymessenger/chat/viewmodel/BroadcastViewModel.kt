package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.network.SocketManager
import com.mistymessenger.core.network.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val contactDao: ContactDao,
    private val socketManager: SocketManager,
    private val tokenProvider: TokenProvider,
    private val newChatViewModel: NewChatViewModel
) : ViewModel() {

    val contacts = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected = _selected.asStateFlow()

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _sentCount = MutableStateFlow(0)
    val sentCount = _sentCount.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val filteredContacts: StateFlow<List<ContactEntity>> = _query
        .combine(contacts) { q, list ->
            if (q.isBlank()) list
            else list.filter { it.name.contains(q, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleContact(userId: String) {
        _selected.update { if (it.contains(userId)) it - userId else it + userId }
    }

    fun setMessage(text: String) { _message.value = text }
    fun setQuery(q: String) { _query.value = q }

    fun send(onDone: () -> Unit) {
        val text = _message.value.trim()
        val recipients = _selected.value.toList()
        if (text.isBlank() || recipients.isEmpty()) return

        viewModelScope.launch {
            _isSending.value = true
            _sentCount.value = 0
            recipients.forEach { userId ->
                runCatching {
                    newChatViewModel.getOrCreateDm(userId)?.let { chatId ->
                        val msgId = UUID.randomUUID().toString()
                        socketManager.emit("message:send", JSONObject().apply {
                            put("id", msgId)
                            put("chatId", chatId)
                            put("type", "text")
                            put("content", text)
                            put("isBroadcast", true)
                        })
                        _sentCount.update { it + 1 }
                    }
                }
            }
            _isSending.value = false
            onDone()
        }
    }
}
