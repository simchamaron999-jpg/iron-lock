package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.chat.repository.ChatSyncRepository
import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.network.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val chatSyncRepository: ChatSyncRepository,
    private val tokenProvider: TokenProvider
) : ViewModel() {

    val chats = chatDao.getActiveChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncChats()
    }

    private fun syncChats() {
        viewModelScope.launch {
            runCatching {
                val myId = tokenProvider.getUserId()
                if (myId.isNotEmpty()) chatSyncRepository.syncChats(myId)
            }
        }
    }

    fun refresh() = syncChats()

    fun onChatLongPress(chatId: String) { /* show bottom sheet */ }

    fun archiveChat(chatId: String) {
        viewModelScope.launch { chatDao.setArchived(chatId, true) }
    }

    fun pinChat(chatId: String, pinned: Boolean) {
        viewModelScope.launch { chatDao.setPinned(chatId, pinned) }
    }

    fun muteChat(chatId: String, muted: Boolean) {
        viewModelScope.launch { chatDao.setMuted(chatId, muted) }
    }
}
