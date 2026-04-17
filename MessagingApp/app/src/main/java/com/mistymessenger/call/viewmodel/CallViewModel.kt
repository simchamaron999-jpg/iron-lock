package com.mistymessenger.call.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.core.db.dao.CallLogDao
import com.mistymessenger.core.db.entity.CallLogEntity
import com.mistymessenger.core.network.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class CallState(
    val remoteName: String = "",
    val remoteAvatarUrl: String = "",
    val statusText: String = "Calling...",
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isCameraOff: Boolean = false,
    val isConnected: Boolean = false
)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callLogDao: CallLogDao,
    private val socketManager: SocketManager
) : ViewModel() {

    val callLogs: StateFlow<List<CallLogEntity>> = callLogDao.getAllCalls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _callState = MutableStateFlow(CallState())
    val callState = _callState.asStateFlow()

    fun startCall(chatId: String, isVideo: Boolean) {
        viewModelScope.launch {
            socketManager.emit("call:initiate", JSONObject().apply {
                put("chatId", chatId)
                put("type", if (isVideo) "video" else "voice")
            })
            // Phase 6: WebRTC PeerConnection setup here
        }
    }

    fun endCall() {
        viewModelScope.launch {
            socketManager.emit("call:end")
            // Phase 6: close PeerConnection, release media tracks
        }
    }

    fun toggleMute() = _callState.update { it.copy(isMuted = !it.isMuted) }
    fun toggleSpeaker() = _callState.update { it.copy(isSpeaker = !it.isSpeaker) }
    fun toggleCamera() = _callState.update { it.copy(isCameraOff = !it.isCameraOff) }
    fun flipCamera() { /* Phase 6: switch front/back camera */ }
}
