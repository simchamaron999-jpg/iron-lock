package com.mistymessenger.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mistymessenger.core.db.entity.MessageEntity

data class MessageAction(
    val icon: ImageVector,
    val label: String,
    val isDestructive: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenu(
    message: MessageEntity,
    isOutgoing: Boolean,
    onReply: () -> Unit,
    onReact: () -> Unit,
    onStar: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        ) {
            // Quick reaction strip at top
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                    Box(
                        modifier = Modifier.size(44.dp).clickable { onReact(); onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            HorizontalDivider()

            MenuActionRow(Icons.Default.Reply, "Reply") { onReply(); onDismiss() }
            MenuActionRow(Icons.Default.EmojiEmotions, "React") { onReact(); onDismiss() }
            MenuActionRow(
                if (message.isStarred) Icons.Default.StarBorder else Icons.Default.Star,
                if (message.isStarred) "Unstar" else "Star"
            ) { onStar(); onDismiss() }
            MenuActionRow(Icons.Default.Forward, "Forward") { onForward(); onDismiss() }
            if (message.type == "text") {
                MenuActionRow(Icons.Default.ContentCopy, "Copy") { onCopy(); onDismiss() }
            }
            HorizontalDivider()
            MenuActionRow(Icons.Default.Delete, "Delete for me", isDestructive = true) { onDeleteForMe(); onDismiss() }
            if (isOutgoing) {
                MenuActionRow(Icons.Default.DeleteForever, "Delete for everyone", isDestructive = true) { onDeleteForEveryone(); onDismiss() }
            }
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = { Text(label, color = tint) },
        leadingContent = { Icon(icon, label, tint = tint) }
    )
}
