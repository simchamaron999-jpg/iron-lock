package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.db.entity.ChatEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatDao: ChatDao
) : ViewModel() {

    val chats = chatDao.getActiveChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onChatLongPress(chatId: String) {
        // Show bottom sheet: mute, archive, pin, delete
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch { chatDao.setArchived(chatId, true) }
    }

    fun pinChat(chatId: String, pinned: Boolean) {
        viewModelScope.launch { chatDao.setPinned(chatId, pinned) }
    }
}
