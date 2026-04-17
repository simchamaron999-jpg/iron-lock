package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.entity.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StarredMessagesViewModel @Inject constructor(
    messageDao: MessageDao
) : ViewModel() {

    val starredMessages = messageDao.getStarredMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
