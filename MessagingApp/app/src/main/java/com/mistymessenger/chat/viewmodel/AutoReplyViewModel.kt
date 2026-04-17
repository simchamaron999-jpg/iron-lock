package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.AutoReplyDao
import com.mistymessenger.core.db.entity.AutoReplyEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AutoReplyViewModel @Inject constructor(
    private val autoReplyDao: AutoReplyDao
) : ViewModel() {

    val rules: StateFlow<List<AutoReplyEntity>> = autoReplyDao.getEnabledRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(trigger: String, response: String, scope: String = "all") {
        viewModelScope.launch {
            autoReplyDao.insert(
                AutoReplyEntity(
                    id = UUID.randomUUID().toString(),
                    trigger = trigger,
                    response = response,
                    scope = scope
                )
            )
        }
    }

    fun toggleRule(id: String, enabled: Boolean) {
        viewModelScope.launch { autoReplyDao.setEnabled(id, enabled) }
    }

    fun deleteRule(rule: AutoReplyEntity) {
        viewModelScope.launch { autoReplyDao.delete(rule) }
    }
}
