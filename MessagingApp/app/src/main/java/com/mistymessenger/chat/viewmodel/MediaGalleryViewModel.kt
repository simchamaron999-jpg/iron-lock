package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.entity.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    private val messageDao: MessageDao
) : ViewModel() {

    private val _mediaMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val mediaMessages = _mediaMessages.asStateFlow()

    fun load(chatId: String) {
        viewModelScope.launch {
            messageDao.getMediaMessages(chatId).collect {
                _mediaMessages.value = it
            }
        }
    }
}
