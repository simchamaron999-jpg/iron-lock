package com.mistymessenger.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun VoiceRecorderBar(
    durationMs: Long,
    amplitudes: List<Int>,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.8f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(600), repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, "Cancel", tint = MaterialTheme.colorScheme.onErrorContainer)
            }

            Icon(
                Icons.Default.Mic,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp * pulse)
            )
            Spacer(Modifier.width(8.dp))

            Text(
                formatDuration(durationMs),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.weight(1f))

            WaveformPreview(amplitudes = amplitudes, modifier = Modifier.width(80.dp).height(32.dp))
            Spacer(Modifier.width(8.dp))

            FilledIconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send voice message")
            }
        }
    }
}

@Composable
fun WaveformPreview(amplitudes: List<Int>, modifier: Modifier = Modifier) {

    val max = amplitudes.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val display = if (amplitudes.size > 20) {
            val step = amplitudes.size / 20
            (0 until 20).map { i -> amplitudes[i * step] }
        } else amplitudes

        display.forEach { amp ->
            val fraction = amp.toFloat() / max
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction.coerceIn(0.1f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun WaveformPlayback(amplitudes: List<Int>, progress: Float, modifier: Modifier = Modifier) {
    val max = amplitudes.maxOrNull()?.coerceAtLeast(1) ?: 1
    val count = amplitudes.size.coerceAtLeast(20)
    val playedCount = (progress * count).toInt()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        amplitudes.forEachIndexed { idx, amp ->
            val fraction = amp.toFloat() / max
            val color = if (idx < playedCount)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction.coerceIn(0.1f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
