package com.mistymessenger.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.AccountDao
import com.mistymessenger.core.db.entity.AccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyState(
    val showLastSeen: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val showReadReceipts: Boolean = true,
    val showTyping: Boolean = true,
    val ghostMode: Boolean = false,
    val freezeLastSeen: Boolean = false,
    val antiDelete: Boolean = true,
    val antiRevoke: Boolean = true
)

data class ThemePrefs(
    val isDark: Boolean = false,
    val useDynamicColor: Boolean = true,
    val seedColorHex: String = "#00BFA5",
    val fontName: String = "Roboto"
)

data class AppLockState(
    val enabled: Boolean = false,
    val useBiometric: Boolean = true,
    val pin: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountDao: AccountDao
) : ViewModel() {

    private val _privacyState = MutableStateFlow(PrivacyState())
    val privacyState = _privacyState.asStateFlow()

    private val _themePrefs = MutableStateFlow(ThemePrefs())
    val themePrefs = _themePrefs.asStateFlow()

    private val _appLockState = MutableStateFlow(AppLockState())
    val appLockState = _appLockState.asStateFlow()

    val accounts: StateFlow<List<AccountEntity>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Privacy
    fun setShowLastSeen(v: Boolean) = _privacyState.update { it.copy(showLastSeen = v) }
    fun setShowOnlineStatus(v: Boolean) = _privacyState.update { it.copy(showOnlineStatus = v) }
    fun setShowReadReceipts(v: Boolean) = _privacyState.update { it.copy(showReadReceipts = v) }
    fun setShowTyping(v: Boolean) = _privacyState.update { it.copy(showTyping = v) }
    fun setGhostMode(v: Boolean) = _privacyState.update { it.copy(ghostMode = v) }
    fun setFreezeLastSeen(v: Boolean) = _privacyState.update { it.copy(freezeLastSeen = v) }
    fun setAntiDelete(v: Boolean) = _privacyState.update { it.copy(antiDelete = v) }
    fun setAntiRevoke(v: Boolean) = _privacyState.update { it.copy(antiRevoke = v) }

    // Theme
    fun setDarkMode(v: Boolean) = _themePrefs.update { it.copy(isDark = v) }
    fun setDynamicColor(v: Boolean) = _themePrefs.update { it.copy(useDynamicColor = v) }
    fun setSeedColor(hex: String) = _themePrefs.update { it.copy(seedColorHex = hex) }
    fun setFont(name: String) = _themePrefs.update { it.copy(fontName = name) }

    // App lock
    fun setAppLockEnabled(v: Boolean) = _appLockState.update { it.copy(enabled = v) }
    fun setUseBiometric(v: Boolean) = _appLockState.update { it.copy(useBiometric = v) }

    // Accounts
    fun switchAccount(id: String) {
        viewModelScope.launch {
            accountDao.deactivateAll()
            accountDao.setActive(id)
        }
    }
}
