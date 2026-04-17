package com.mistymessenger.core.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.network.SocketManager
import com.mistymessenger.core.network.TokenProvider
import com.mistymessenger.core.ui.theme.AppFonts
import com.mistymessenger.core.ui.theme.MistyGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeState(
    val isDark: Boolean = false,
    val seedColor: Color = MistyGreen,
    val fontFamily: FontFamily = AppFonts.Roboto,
    val useDynamicColor: Boolean = true
)

data class AuthState(
    val isLoggedIn: Boolean = false,
    val userId: String = ""
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _themeState = MutableStateFlow(ThemeState())
    val themeState = _themeState.asStateFlow()

    private val _authState = MutableStateFlow(AuthState())
    val authState = _authState.asStateFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val token = tokenProvider.getAccessToken()
            val userId = tokenProvider.getUserId()
            _authState.update { it.copy(isLoggedIn = token.isNotEmpty(), userId = userId) }
            if (token.isNotEmpty()) socketManager.connect()
        }
    }

    fun updateTheme(isDark: Boolean? = null, seedColor: Color? = null, fontFamily: FontFamily? = null) {
        _themeState.update { current ->
            current.copy(
                isDark = isDark ?: current.isDark,
                seedColor = seedColor ?: current.seedColor,
                fontFamily = fontFamily ?: current.fontFamily
            )
        }
    }

    fun onAppForeground() {
        if (_authState.value.isLoggedIn && !socketManager.isConnected) {
            socketManager.connect()
        }
    }

    fun onAppBackground() {
        // Presence handled server-side via heartbeat timeout
    }
}
