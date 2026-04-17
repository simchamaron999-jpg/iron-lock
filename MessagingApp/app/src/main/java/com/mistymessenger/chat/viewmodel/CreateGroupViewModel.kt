package com.mistymessenger.chat.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.chat.repository.GroupRepository
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.entity.ContactEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val contactDao: ContactDao,
    private val groupRepository: GroupRepository
) : ViewModel() {

    val contacts = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<Set<String>>(emptySet()) // userIds
    val selected = _selected.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName = _groupName.asStateFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri = _avatarUri.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating = _isCreating.asStateFlow()

    private val _createdChatId = MutableStateFlow<String?>(null)
    val createdChatId = _createdChatId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val filteredContacts: StateFlow<List<ContactEntity>> = _query
        .combine(contacts) { q, list ->
            if (q.isBlank()) list
            else list.filter { it.name.contains(q, ignoreCase = true) || it.phone.contains(q) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleContact(userId: String) {
        _selected.update { if (it.contains(userId)) it - userId else it + userId }
    }

    fun setGroupName(name: String) { _groupName.value = name }
    fun setAvatarUri(uri: Uri?) { _avatarUri.value = uri }
    fun setQuery(q: String) { _query.value = q }

    fun createGroup(onCreated: (chatId: String) -> Unit) {
        val name = _groupName.value.trim()
        if (name.isBlank()) { _error.value = "Group name required"; return }
        if (_selected.value.size < 1) { _error.value = "Select at least 1 member"; return }

        viewModelScope.launch {
            _isCreating.value = true
            runCatching {
                groupRepository.createGroup(name, _selected.value.toList(), _avatarUri.value)
            }.onSuccess { entity ->
                _isCreating.value = false
                onCreated(entity.id)
            }.onFailure { e ->
                _isCreating.value = false
                _error.value = e.message ?: "Failed to create group"
            }
        }
    }

    fun clearError() { _error.value = null }
}
