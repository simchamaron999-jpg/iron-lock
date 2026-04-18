package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.dao.UserDao
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.db.entity.UserEntity
import com.mistymessenger.core.network.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import javax.inject.Inject

@Serializable
data class UserProfileResponse(
    val id: String,
    val phone: String,
    val name: String,
    val avatarUrl: String = "",
    val bio: String = "",
    val lastSeen: String? = null,
    val isOnline: Boolean = false
)

interface UserApiService {
    @GET("users/{id}") suspend fun getUser(@Path("id") id: String): UserProfileResponse
}

data class ContactInfoUiState(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val isOnline: Boolean = false,
    val lastSeen: String = "",
    val isBlocked: Boolean = false,
    val chatId: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ContactInfoViewModel @Inject constructor(
    private val userDao: UserDao,
    private val contactDao: ContactDao,
    retrofitClient: RetrofitClient
) : ViewModel() {

    private val userApi = retrofitClient.retrofit.create(UserApiService::class.java)
    private val chatApi = retrofitClient.retrofit.create(ChatApiService::class.java)
    private val _state = MutableStateFlow(ContactInfoUiState())
    val state = _state.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            // Load from local Room first for instant display
            val contact = contactDao.getContactById(userId)
            if (contact != null) {
                _state.update { it.copy(
                    userId = userId,
                    name = contact.name,
                    phone = contact.phone,
                    avatarUrl = contact.avatarUrl,
                    isBlocked = contact.isBlocked,
                    isLoading = false
                ) }
            }

            // Fetch live data from server
            runCatching { userApi.getUser(userId) }.onSuccess { profile ->
                _state.update { it.copy(
                    userId = profile.id,
                    name = if (contact?.name?.isNotBlank() == true) contact.name else profile.name,
                    phone = profile.phone,
                    avatarUrl = profile.avatarUrl,
                    bio = profile.bio,
                    isOnline = profile.isOnline,
                    lastSeen = profile.lastSeen ?: "",
                    isLoading = false
                ) }
                // Update local Room cache
                userDao.insert(UserEntity(
                    id = profile.id,
                    phone = profile.phone,
                    name = profile.name,
                    avatarUrl = profile.avatarUrl,
                    bio = profile.bio,
                    isOnline = profile.isOnline
                ))
            }

            // Pre-fetch or create DM chat ID
            runCatching { chatApi.createOrGetDm(CreateDmRequest(userId)).id }
                .onSuccess { chatId -> _state.update { it.copy(chatId = chatId) } }
        }
    }

    fun blockContact(userId: String) {
        viewModelScope.launch {
            contactDao.setBlocked(userId, true)
            _state.update { it.copy(isBlocked = true) }
        }
    }

    fun unblockContact(userId: String) {
        viewModelScope.launch {
            contactDao.setBlocked(userId, false)
            _state.update { it.copy(isBlocked = false) }
        }
    }
}
