package com.mistymessenger.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.entity.MessageEntity
import kotlinx.coroutines.flow.flow

@Composable
fun ReplyQuote(
    replyToId: String,
    messageDao: MessageDao,
    modifier: Modifier = Modifier
) {
    val message by flow<MessageEntity?> {
        emit(messageDao.getById(replyToId))
    }.collectAsState(initial = null)

    message?.let { msg ->
        ReplyQuoteContent(message = msg, modifier = modifier)
    }
}

@Composable
fun ReplyQuoteContent(message: MessageEntity, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))

        // Thumbnail for media types
        if (message.type == "image" && message.thumbnailUrl.isNotEmpty()) {
            AsyncImage(
                model = message.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Reply",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (message.type) {
                    "image" -> Icon(Icons.Default.Photo, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    "video" -> Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    "audio" -> Icon(Icons.Default.Mic, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    "document" -> Icon(Icons.Default.InsertDriveFile, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val preview = when {
                    message.isDeletedForEveryone -> "This message was deleted"
                    message.type == "text" -> message.content
                    message.type == "image" -> "Photo"
                    message.type == "video" -> "Video"
                    message.type == "audio" -> "Voice message"
                    message.type == "document" -> "Document"
                    else -> message.content
                }
                Text(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
