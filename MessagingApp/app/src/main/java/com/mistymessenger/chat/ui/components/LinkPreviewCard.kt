package com.mistymessenger.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mistymessenger.core.db.entity.MessageEntity

@Composable
fun LinkPreviewCard(message: MessageEntity, modifier: Modifier = Modifier) {
    if (message.linkPreviewUrl.isBlank()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(8.dp)
    ) {
        if (message.linkPreviewImageUrl.isNotBlank()) {
            AsyncImage(
                model = message.linkPreviewImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.height(6.dp))
        }
        if (message.linkPreviewTitle.isNotBlank()) {
            Text(
                message.linkPreviewTitle,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (message.linkPreviewDescription.isNotBlank()) {
            Text(
                message.linkPreviewDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            message.linkPreviewUrl,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
