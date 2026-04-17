package com.mistymessenger.call.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.mistymessenger.core.ui.components.AvatarImage

data class IncomingCallInfo(
    val callId: String,
    val chatId: String,
    val fromUserId: String,
    val fromName: String,
    val fromAvatarUrl: String,
    val isVideo: Boolean,
    val sdp: String
)

@Composable
fun IncomingCallScreen(
    call: IncomingCallInfo,
    onAccept: (IncomingCallInfo) -> Unit,
    onDecline: (IncomingCallInfo) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.inverseSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(80.dp))
                Text(
                    if (call.isVideo) "Incoming video call" else "Incoming voice call",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(24.dp))
                AvatarImage(url = call.fromAvatarUrl, name = call.fromName.ifBlank { "?" }, size = 120.dp)
                Spacer(Modifier.height(16.dp))
                Text(call.fromName.ifBlank { "Unknown" }, color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onDecline(call) },
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.Red)
                    ) { Icon(Icons.Default.CallEnd, "Decline", tint = Color.White) }
                    Spacer(Modifier.height(4.dp))
                    Text("Decline", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onAccept(call) },
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF2ECC71))
                    ) {
                        Icon(
                            if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            "Accept", tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Accept", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
