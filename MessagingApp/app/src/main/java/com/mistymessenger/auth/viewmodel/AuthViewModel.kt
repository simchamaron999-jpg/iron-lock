package com.mistymessenger.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.auth.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(val isLoading: Boolean = false, val error: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun sendOTP(phone: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.sendOTP(phone)
                .onSuccess { onSent() }
                .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun verifyOTP(phone: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.verifyOTP(phone, otp)
                .onSuccess { onSuccess() }
                .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun saveProfile(name: String, bio: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.saveProfile(name, bio)
                .onSuccess { onDone() }
                .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
