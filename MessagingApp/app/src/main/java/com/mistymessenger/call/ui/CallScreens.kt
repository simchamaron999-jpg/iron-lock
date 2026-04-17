package com.mistymessenger.call.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mistymessenger.call.viewmodel.CallViewModel
import com.mistymessenger.core.db.entity.CallLogEntity
import com.mistymessenger.core.ui.components.AvatarImage

@Composable
fun CallsScreen(
    onCallClick: (String, Boolean) -> Unit,
    onNewCall: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val calls by viewModel.callLogs.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewCall) {
                Icon(Icons.Default.AddCall, "New call")
            }
        }
    ) { padding ->
        if (calls.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No recent calls", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(calls.size) { i ->
                    CallLogItem(call = calls[i], onCallBack = { onCallClick(calls[i].chatId, calls[i].type == "video") })
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun CallLogItem(call: CallLogEntity, onCallBack: () -> Unit) {
    ListItem(
        headlineContent = { Text(call.participantIds.firstOrNull() ?: "Unknown") },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    when (call.direction) {
                        "incoming" -> Icons.Default.CallReceived
                        "outgoing" -> Icons.Default.CallMade
                        else -> Icons.Default.CallMissed
                    },
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = if (call.direction == "missed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    call.direction.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (call.direction == "missed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = { AvatarImage(url = "", name = call.participantIds.firstOrNull() ?: "?", size = 48.dp) },
        trailingContent = {
            IconButton(onClick = onCallBack) {
                Icon(
                    if (call.type == "video") Icons.Default.Videocam else Icons.Default.Call,
                    "Call back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCallScreen(chatId: String, onEnd: () -> Unit, viewModel: CallViewModel = hiltViewModel()) {
    val state by viewModel.callState.collectAsState()

    LaunchedEffect(chatId) { viewModel.startCall(chatId, isVideo = false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.inverseSurface)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(80.dp))
                AvatarImage(url = state.remoteAvatarUrl, name = state.remoteName, size = 100.dp)
                Spacer(Modifier.height(16.dp))
                Text(state.remoteName, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(state.statusText, color = Color.White.copy(alpha = 0.7f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallButton(Icons.Default.MicOff, "Mute", state.isMuted) { viewModel.toggleMute() }
                CallButton(Icons.Default.CallEnd, "End", false, Color.Red) { viewModel.endCall(); onEnd() }
                CallButton(Icons.Default.VolumeUp, "Speaker", state.isSpeaker) { viewModel.toggleSpeaker() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallScreen(chatId: String, onEnd: () -> Unit, viewModel: CallViewModel = hiltViewModel()) {
    val state by viewModel.callState.collectAsState()

    LaunchedEffect(chatId) { viewModel.startCall(chatId, isVideo = true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Remote video view fills screen (WebRTC SurfaceViewRenderer — wired in Phase 6)
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
            Text("Remote video", color = Color.White.copy(alpha = 0.3f))
        }
        // Local video preview (PiP)
        Surface(
            modifier = Modifier.size(100.dp, 140.dp).align(Alignment.TopEnd).padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color(0xFF16213E)
        ) {
            Box(contentAlignment = Alignment.Center) { Text("You", color = Color.White.copy(alpha = 0.5f)) }
        }
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallButton(Icons.Default.MicOff, "Mute", state.isMuted) { viewModel.toggleMute() }
            CallButton(Icons.Default.VideocamOff, "Camera", state.isCameraOff) { viewModel.toggleCamera() }
            CallButton(Icons.Default.CallEnd, "End", false, Color.Red) { viewModel.endCall(); onEnd() }
            CallButton(Icons.Default.Cameraswitch, "Flip", false) { viewModel.flipCamera() }
        }
    }
}

@Composable
private fun CallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(if (active) activeColor else Color.White.copy(alpha = 0.15f))
        ) {
            Icon(icon, label, tint = if (active) Color.White else Color.White)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}
