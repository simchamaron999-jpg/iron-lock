package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.db.entity.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val chats: List<ChatEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
@OptIn(FlowPreview::class)
class GlobalSearchViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .filter { it.length >= 2 }
                .collect { q ->
                    _state.update { it.copy(isLoading = true) }
                    val chats = chatDao.searchChats(q)
                    val contacts = contactDao.searchContacts(q).first()
                    val messages = messageDao.searchMessages(q)
                    _state.value = SearchState(chats, contacts, messages)
                }
        }
    }

    fun search(query: String) {
        _query.value = query
        if (query.length < 2) {
            _state.value = SearchState()
        }
    }
}
