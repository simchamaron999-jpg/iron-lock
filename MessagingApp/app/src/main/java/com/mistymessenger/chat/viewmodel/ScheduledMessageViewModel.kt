package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.ScheduledMessageDao
import com.mistymessenger.core.db.entity.ScheduledMessageEntity
import com.mistymessenger.core.network.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ScheduledMessageViewModel @Inject constructor(
    private val scheduledMessageDao: ScheduledMessageDao,
    private val socketManager: SocketManager
) : ViewModel() {

    val pendingMessages: StateFlow<List<ScheduledMessageEntity>> = scheduledMessageDao.getPending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun schedule(chatId: String, content: String, scheduledAtMs: Long) {
        viewModelScope.launch {
            val msg = ScheduledMessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                content = content,
                scheduledAt = scheduledAtMs
            )
            scheduledMessageDao.insert(msg)

            // POST to backend BullMQ queue
            socketManager.emit("message:schedule", JSONObject().apply {
                put("localId", msg.id)
                put("chatId", chatId)
                put("content", content)
                put("type", "text")
                put("scheduledAt", scheduledAtMs)
            })
        }
    }

    fun cancel(msgId: String) {
        viewModelScope.launch {
            scheduledMessageDao.cancel(msgId)
            socketManager.emit("message:schedule_cancel", JSONObject().apply { put("localId", msgId) })
        }
    }
}
