package com.mistymessenger.core.network

import android.util.Log
import com.mistymessenger.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val tokenProvider: TokenProvider
) {
    private var socket: Socket? = null

    fun connect() {
        val opts = IO.Options().apply {
            auth = mapOf("token" to tokenProvider.getAccessToken())
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1000
        }
        socket = IO.socket(URI.create(BuildConfig.SOCKET_URL), opts).apply {
            on(Socket.EVENT_CONNECT) { Log.d("Socket", "Connected") }
            on(Socket.EVENT_DISCONNECT) { args -> Log.d("Socket", "Disconnected: ${args[0]}") }
            on(Socket.EVENT_CONNECT_ERROR) { args -> Log.e("Socket", "Error: ${args[0]}") }
            connect()
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    fun emit(event: String, data: JSONObject) {
        socket?.emit(event, data)
    }

    fun emit(event: String) {
        socket?.emit(event)
    }

    fun listenFlow(event: String): Flow<JSONObject> = callbackFlow {
        val listener = io.socket.emitter.Emitter.Listener { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                trySend(args[0] as JSONObject)
            }
        }
        socket?.on(event, listener)
        awaitClose { socket?.off(event, listener) }
    }

    val isConnected: Boolean get() = socket?.connected() == true
}
