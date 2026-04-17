package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.network.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject

@Serializable data class CreateDmRequest(val userId: String)
@Serializable data class ChatIdResponse(val id: String)

interface ChatApiService {
    @POST("chats/dm") suspend fun createOrGetDm(@Body req: CreateDmRequest): ChatIdResponse
}

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NewChatViewModel @Inject constructor(
    private val contactDao: ContactDao,
    retrofitClient: RetrofitClient
) : ViewModel() {

    private val api = retrofitClient.retrofit.create(ChatApiService::class.java)

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val contacts: StateFlow<List<ContactEntity>> = _query
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) contactDao.getAllContacts()
            else contactDao.searchContacts(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }

    fun openOrCreateDm(userId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { api.createOrGetDm(CreateDmRequest(userId)) }
                .onSuccess { onResult(it.id) }
        }
    }

    suspend fun getOrCreateDm(userId: String): String? =
        runCatching { api.createOrGetDm(CreateDmRequest(userId)).id }.getOrNull()
}
