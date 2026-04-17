package com.mistymessenger.call.webrtc

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton wrapper around WebRTC PeerConnectionFactory. Owns the current call's
 * PeerConnection, local audio/video tracks, camera capturer, and EglBase for
 * SurfaceViewRenderer sharing. One call at a time.
 */
@Singleton
class WebRtcManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val eglBase: EglBase = EglBase.create()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        val opts = PeerConnectionFactory.Options()
        val encoder = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoder = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        PeerConnectionFactory.builder()
            .setOptions(opts)
            .setVideoEncoderFactory(encoder)
            .setVideoDecoderFactory(decoder)
            .createPeerConnectionFactory()
    }

    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var useFrontCamera: Boolean = true

    var onIceCandidate: ((IceCandidate) -> Unit)? = null
    var onRemoteStream: ((MediaStream) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    fun createPeerConnection(isVideo: Boolean) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                onIceCandidate?.invoke(candidate)
            }

            override fun onAddStream(stream: MediaStream) {
                onRemoteStream?.invoke(stream)
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> onConnected?.invoke()
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.CLOSED -> onDisconnected?.invoke()
                    else -> Unit
                }
            }

            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })

        // Local audio
        audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("MM_AUDIO", audioSource).apply { setEnabled(true) }
        peerConnection?.addTrack(localAudioTrack, listOf("MM_STREAM"))

        if (isVideo) {
            videoCapturer = createCameraCapturer(useFrontCamera)
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            videoSource = factory.createVideoSource(false)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30)
            localVideoTrack = factory.createVideoTrack("MM_VIDEO", videoSource).apply { setEnabled(true) }
            peerConnection?.addTrack(localVideoTrack, listOf("MM_STREAM"))
        }
    }

    fun attachLocalView(renderer: SurfaceViewRenderer) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setMirror(useFrontCamera)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        localVideoTrack?.addSink(renderer)
    }

    fun attachRemoteView(renderer: SurfaceViewRenderer, stream: MediaStream) {
        if (!renderer.isInitialized()) renderer.init(eglBase.eglBaseContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        stream.videoTracks.firstOrNull()?.addSink(renderer)
    }

    private fun SurfaceViewRenderer.isInitialized(): Boolean =
        runCatching { this.resources != null }.getOrDefault(false)

    fun createOffer(isVideo: Boolean, onSdp: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
        }
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                onSdp(sdp)
            }
        }, constraints)
    }

    fun createAnswer(isVideo: Boolean, onSdp: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
        }
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                onSdp(sdp)
            }
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) { useFrontCamera = isFront }
            override fun onCameraSwitchError(error: String?) {}
        })
    }

    private fun createCameraCapturer(front: Boolean): CameraVideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(context))
            Camera2Enumerator(context) else Camera1Enumerator(false)
        val deviceNames = enumerator.deviceNames
        val primary = deviceNames.firstOrNull {
            if (front) enumerator.isFrontFacing(it) else enumerator.isBackFacing(it)
        } ?: deviceNames.firstOrNull() ?: return null
        return enumerator.createCapturer(primary, null)
    }

    fun close() {
        runCatching { videoCapturer?.stopCapture() }
        videoCapturer?.dispose(); videoCapturer = null
        videoSource?.dispose(); videoSource = null
        audioSource?.dispose(); audioSource = null
        surfaceTextureHelper?.dispose(); surfaceTextureHelper = null
        localVideoTrack = null
        localAudioTrack = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        onIceCandidate = null
        onRemoteStream = null
        onConnected = null
        onDisconnected = null
    }
}

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}
