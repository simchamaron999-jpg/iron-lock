package com.mistymessenger.chat.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.chat.repository.GroupRepository
import com.mistymessenger.chat.repository.GroupResponse
import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.network.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupInfoState(
    val chat: ChatEntity? = null,
    val contacts: List<ContactEntity> = emptyList(),
    val inviteLink: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupRepository: GroupRepository,
    private val tokenProvider: TokenProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val currentUserId: String get() = tokenProvider.getUserId()

    private val _state = MutableStateFlow(GroupInfoState())
    val state = _state.asStateFlow()

    fun load(chatId: String) {
        viewModelScope.launch {
            chatDao.getChatById(chatId).collect { chat ->
                _state.update { it.copy(chat = chat) }
                chat?.memberIds?.let { ids ->
                    val contacts = contactDao.getContactsByIds(ids)
                    _state.update { it.copy(contacts = contacts) }
                }
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { groupRepository.getGroupInfo(chatId) }
                .onSuccess { resp ->
                    _state.update { it.copy(isLoading = false, inviteLink = resp.inviteLink ?: "") }
                    chatDao.updateAdmins(chatId, resp.adminIds)
                    chatDao.updateMembers(chatId, resp.memberIds)
                }
                .onFailure { _state.update { s -> s.copy(isLoading = false) } }
        }
    }

    fun generateInviteLink(chatId: String) {
        viewModelScope.launch {
            runCatching { groupRepository.generateInviteLink(chatId) }
                .onSuccess { link ->
                    _state.update { it.copy(inviteLink = link) }
                    chatDao.updateInviteLink(chatId, link)
                }
        }
    }

    fun copyInviteLink() {
        val link = _state.value.inviteLink
        if (link.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("invite", link))
    }

    fun addMember(chatId: String, userId: String) {
        viewModelScope.launch {
            runCatching { groupRepository.addMember(chatId, userId) }
                .onSuccess { resp -> chatDao.updateMembers(chatId, resp.memberIds) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun removeMember(chatId: String, userId: String) {
        viewModelScope.launch {
            runCatching { groupRepository.removeMember(chatId, userId) }
                .onSuccess { resp -> chatDao.updateMembers(chatId, resp.memberIds) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun promoteAdmin(chatId: String, userId: String) {
        viewModelScope.launch {
            runCatching { groupRepository.promoteAdmin(chatId, userId) }
                .onSuccess { resp -> chatDao.updateAdmins(chatId, resp.adminIds) }
        }
    }

    fun demoteAdmin(chatId: String, userId: String) {
        viewModelScope.launch {
            runCatching { groupRepository.demoteAdmin(chatId, userId) }
                .onSuccess { resp -> chatDao.updateAdmins(chatId, resp.adminIds) }
        }
    }

    fun leaveGroup(chatId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { groupRepository.leaveGroup(chatId) }
                .onSuccess { onDone() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }
}
