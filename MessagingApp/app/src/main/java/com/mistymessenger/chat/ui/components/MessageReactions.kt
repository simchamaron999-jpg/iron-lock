package com.mistymessenger.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// reactions stored as "userId:emoji" list
@Composable
fun MessageReactions(
    reactions: List<String>,
    currentUserId: String,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    // Group by emoji and count
    val grouped = reactions
        .mapNotNull { r -> r.split(":").takeIf { it.size == 2 }?.let { it[0] to it[1] } }
        .groupBy { it.second }
        .map { (emoji, pairs) ->
            Triple(emoji, pairs.size, pairs.any { it.first == currentUserId })
        }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(grouped) { (emoji, count, isMine) ->
            ReactionChip(
                emoji = emoji,
                count = count,
                isMine = isMine,
                onClick = { onReactionClick(emoji) }
            )
        }
    }
}

@Composable
fun ReactionChip(emoji: String, count: Int, isMine: Boolean, onClick: () -> Unit) {
    val bgColor = if (isMine)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(emoji, fontSize = 13.sp)
        if (count > 1) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
