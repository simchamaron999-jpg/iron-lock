package com.mistymessenger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mistymessenger.core.ui.theme.TickRead
import com.mistymessenger.core.ui.theme.TickSent

@Composable
fun AvatarImage(
    url: String,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    if (url.isNotEmpty()) {
        AsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )
    } else {
        val initial = name.take(1).uppercase().ifEmpty { "?" }
        val color = avatarColor(name)
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initial,
                color = Color.White,
                style = when {
                    size >= 60.dp -> MaterialTheme.typography.headlineSmall
                    size >= 40.dp -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.labelLarge
                }
            )
        }
    }
}

private fun avatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF1565C0), Color(0xFF00695C), Color(0xFF4527A0),
        Color(0xFF558B2F), Color(0xFFAD1457), Color(0xFF00838F),
        Color(0xFF6D4C41), Color(0xFF283593)
    )
    return colors[(name.hashCode().and(0x7FFFFFFF)) % colors.size]
}

@Composable
fun MessageTickIcon(status: String, modifier: Modifier = Modifier) {
    when (status) {
        "sending" -> Icon(Icons.Default.Schedule, "Sending", modifier = modifier.size(14.dp), tint = TickSent)
        "sent" -> Icon(Icons.Default.Done, "Sent", modifier = modifier.size(14.dp), tint = TickSent)
        "delivered" -> Icon(Icons.Default.DoneAll, "Delivered", modifier = modifier.size(14.dp), tint = TickSent)
        "read" -> Icon(Icons.Default.DoneAll, "Read", modifier = modifier.size(14.dp), tint = TickRead)
    }
}
