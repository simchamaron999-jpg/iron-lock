package com.mistymessenger.status.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.StatusDao
import com.mistymessenger.core.db.entity.StatusEntity
import com.mistymessenger.core.network.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusDao: StatusDao,
    private val tokenProvider: TokenProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val myName: String get() = "Me"
    val myAvatarUrl: String get() = ""

    val myStatuses: StateFlow<List<StatusEntity>> = statusDao
        .getActiveStatuses()
        .map { it.filter { s -> s.userId == tokenProvider.getUserId() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val contactStatuses: StateFlow<List<Pair<String, List<StatusEntity>>>> = statusDao
        .getActiveStatuses()
        .map { all ->
            val myId = tokenProvider.getUserId()
            all.filter { it.userId != myId }
                .groupBy { it.userId }
                .map { (uid, statuses) -> uid to statuses.sortedBy { it.createdAt } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun getStatusesForUser(userId: String) = statusDao.getStatusesForUser(userId)

    fun downloadStatus(status: StatusEntity) {
        viewModelScope.launch {
            // Phase 7: download status.mediaUrl to gallery
        }
    }
}
