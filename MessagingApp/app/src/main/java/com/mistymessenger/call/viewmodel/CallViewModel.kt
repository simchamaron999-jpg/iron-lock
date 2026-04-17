package com.mistymessenger.call.viewmodel

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.call.webrtc.WebRtcManager
import com.mistymessenger.core.db.dao.CallLogDao
import com.mistymessenger.core.db.entity.CallLogEntity
import com.mistymessenger.core.network.SocketManager
import com.mistymessenger.core.network.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import java.util.UUID
import javax.inject.Inject

data class CallState(
    val callId: String = "",
    val chatId: String = "",
    val remoteUserId: String = "",
    val remoteName: String = "",
    val remoteAvatarUrl: String = "",
    val statusText: String = "Calling...",
    val isVideo: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isCameraOff: Boolean = false,
    val isConnected: Boolean = false,
    val isEnded: Boolean = false,
    val durationSeconds: Long = 0L,
    val hasRemoteStream: Boolean = false
)

@HiltViewModel
class CallViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val callLogDao: CallLogDao,
    private val socketManager: SocketManager,
    private val tokenProvider: TokenProvider,
    private val webRtc: WebRtcManager
) : ViewModel() {

    val callLogs: StateFlow<List<CallLogEntity>> = callLogDao.getAllCalls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _callState = MutableStateFlow(CallState())
    val callState = _callState.asStateFlow()

    private var remoteStream: MediaStream? = null
    private var pendingRemoteView: SurfaceViewRenderer? = null
    private var durationJob: Job? = null

    private val audioManager: AudioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init { observeSignaling() }

    fun startCall(chatId: String, isVideo: Boolean) {
        val callId = UUID.randomUUID().toString()
        _callState.update { it.copy(callId = callId, chatId = chatId, isVideo = isVideo, statusText = "Calling...") }

        configureAudio(isVideo)
        wireWebRtc(isVideo) { sdp ->
            socketManager.emit("call:offer", JSONObject().apply {
                put("callId", callId)
                put("chatId", chatId)
                put("sdp", sdp.description)
                put("sdpType", sdp.type.canonicalForm())
                put("isVideo", isVideo)
            })
        }

        socketManager.emit("call:initiate", JSONObject().apply {
            put("chatId", chatId)
            put("type", if (isVideo) "video" else "voice")
        })
    }

    fun acceptIncoming(callId: String, chatId: String, remoteUserId: String, remoteSdp: String, isVideo: Boolean) {
        _callState.update {
            it.copy(
                callId = callId, chatId = chatId, remoteUserId = remoteUserId,
                isVideo = isVideo, statusText = "Connecting..."
            )
        }
        configureAudio(isVideo)
        wireWebRtc(isVideo) { /* no-op: offer already sent by remote */ }
        webRtc.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, remoteSdp))
        webRtc.createAnswer(isVideo) { answer ->
            socketManager.emit("call:answer_sdp", JSONObject().apply {
                put("callId", callId)
                put("targetUserId", remoteUserId)
                put("sdp", answer.description)
            })
        }
        socketManager.emit("call:answer", JSONObject().apply { put("callId", callId) })
    }

    fun endCall() {
        val state = _callState.value
        if (state.callId.isNotEmpty()) {
            socketManager.emit("call:end", JSONObject().apply { put("callId", state.callId) })
            viewModelScope.launch {
                callLogDao.insert(
                    CallLogEntity(
                        id = state.callId,
                        chatId = state.chatId,
                        participantIds = listOf(state.remoteUserId),
                        type = if (state.isVideo) "video" else "voice",
                        direction = "outgoing",
                        startedAt = System.currentTimeMillis() - state.durationSeconds * 1000,
                        endedAt = System.currentTimeMillis(),
                        durationSeconds = state.durationSeconds
                    )
                )
            }
        }
        cleanup()
        _callState.update { it.copy(isEnded = true, statusText = "Call ended") }
    }

    fun toggleMute() {
        val next = !_callState.value.isMuted
        webRtc.setAudioEnabled(!next)
        _callState.update { it.copy(isMuted = next) }
    }

    fun toggleSpeaker() {
        val next = !_callState.value.isSpeaker
        audioManager.isSpeakerphoneOn = next
        _callState.update { it.copy(isSpeaker = next) }
    }

    fun toggleCamera() {
        val next = !_callState.value.isCameraOff
        webRtc.setVideoEnabled(!next)
        _callState.update { it.copy(isCameraOff = next) }
    }

    fun flipCamera() = webRtc.switchCamera()

    fun attachLocalView(renderer: SurfaceViewRenderer) = webRtc.attachLocalView(renderer)

    fun attachRemoteView(renderer: SurfaceViewRenderer) {
        val stream = remoteStream
        if (stream != null) webRtc.attachRemoteView(renderer, stream)
        else pendingRemoteView = renderer
    }

    private fun wireWebRtc(isVideo: Boolean, onOffer: (SessionDescription) -> Unit) {
        webRtc.createPeerConnection(isVideo)
        webRtc.onIceCandidate = { candidate ->
            val state = _callState.value
            if (state.remoteUserId.isNotEmpty()) {
                socketManager.emit("call:ice", JSONObject().apply {
                    put("callId", state.callId)
                    put("targetUserId", state.remoteUserId)
                    put("candidate", JSONObject().apply {
                        put("sdp", candidate.sdp)
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                    })
                })
            }
        }
        webRtc.onRemoteStream = { stream ->
            remoteStream = stream
            pendingRemoteView?.let { webRtc.attachRemoteView(it, stream); pendingRemoteView = null }
            _callState.update { it.copy(hasRemoteStream = true) }
        }
        webRtc.onConnected = {
            _callState.update { it.copy(isConnected = true, statusText = "Connected") }
            startDurationTimer()
        }
        webRtc.onDisconnected = { if (!_callState.value.isEnded) endCall() }

        if (_callState.value.remoteUserId.isEmpty()) {
            // caller side: create offer after presence resolves
            webRtc.createOffer(isVideo, onOffer)
        }
    }

    private fun observeSignaling() {
        viewModelScope.launch {
            socketManager.listenFlow("call:initiated").collect { data ->
                _callState.update {
                    it.copy(callId = data.optString("callId"), statusText = "Ringing...")
                }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("call:answered").collect { _ ->
                _callState.update { it.copy(statusText = "Connecting...") }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("call:rejected").collect { _ ->
                _callState.update { it.copy(statusText = "Declined", isEnded = true) }
                cleanup()
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("call:ended").collect { _ ->
                val dur = _callState.value.durationSeconds
                _callState.update { it.copy(statusText = "Call ended", isEnded = true, durationSeconds = dur) }
                cleanup()
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("call:answer_sdp").collect { data ->
                val sdp = data.optString("sdp")
                webRtc.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
                _callState.update { it.copy(remoteUserId = data.optString("fromUserId", it.remoteUserId)) }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("call:offer").collect { data ->
                // Incoming call offer — UI layer uses a separate incoming-call activity
                _callState.update {
                    it.copy(
                        callId = data.optString("callId"),
                        remoteUserId = data.optString("fromUserId"),
                        statusText = "Incoming call"
                    )
                }
            }
        }
        viewModelScope.launch {
            socketManager.listenFlow("call:ice").collect { data ->
                val c = data.optJSONObject("candidate") ?: return@collect
                webRtc.addIceCandidate(
                    IceCandidate(c.optString("sdpMid"), c.optInt("sdpMLineIndex"), c.optString("sdp"))
                )
            }
        }
    }

    private fun configureAudio(isVideo: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = isVideo
        _callState.update { it.copy(isSpeaker = isVideo) }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            var seconds = 0L
            while (true) {
                delay(1000)
                seconds += 1
                _callState.update { it.copy(durationSeconds = seconds) }
            }
        }
    }

    private fun cleanup() {
        durationJob?.cancel(); durationJob = null
        remoteStream = null
        pendingRemoteView = null
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        webRtc.close()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}

fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
