package com.mistymessenger.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.ChatSettingsDao
import com.mistymessenger.core.db.entity.ChatSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val chatSettingsDao: ChatSettingsDao
) : ViewModel() {

    fun getSettings(chatId: String) = chatSettingsDao.getSettings(chatId)

    fun saveWallpaper(chatId: String, wallpaperUri: String, wallpaperColor: Long) {
        viewModelScope.launch {
            val current = chatSettingsDao.getSettingsOnce(chatId)
            chatSettingsDao.upsert((current ?: ChatSettingsEntity(chatId)).copy(
                wallpaperUri = wallpaperUri,
                wallpaperColor = wallpaperColor
            ))
        }
    }

    fun setChatLocked(chatId: String, locked: Boolean) {
        viewModelScope.launch {
            val current = chatSettingsDao.getSettingsOnce(chatId)
            chatSettingsDao.upsert((current ?: ChatSettingsEntity(chatId)).copy(isChatLocked = locked))
        }
    }

    fun setMuted(chatId: String, muted: Boolean, muteUntil: Long = 0L) {
        viewModelScope.launch {
            val current = chatSettingsDao.getSettingsOnce(chatId)
            chatSettingsDao.upsert((current ?: ChatSettingsEntity(chatId)).copy(
                isMuted = muted, muteUntil = muteUntil
            ))
        }
    }
}
