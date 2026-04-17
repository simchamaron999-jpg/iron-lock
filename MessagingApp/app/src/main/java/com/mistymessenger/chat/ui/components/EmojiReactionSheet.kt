package com.mistymessenger.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

private val ALL_EMOJIS = listOf(
    "😀","😃","😄","😁","😆","😅","😂","🤣","😊","😇",
    "🙂","🙃","😉","😌","😍","🥰","😘","😗","😙","😚",
    "😋","😛","😝","😜","🤪","🤨","🧐","🤓","😎","🥸",
    "🤩","🥳","😏","😒","😞","😔","😟","😕","🙁","☹️",
    "😣","😖","😫","😩","🥺","😢","😭","😤","😠","😡",
    "🤬","🤯","😳","🥵","🥶","😱","😨","😰","😥","😓",
    "🤗","🤔","🤭","🤫","🤥","😶","😐","😑","😬","🙄",
    "😯","😦","😧","😮","😲","🥱","😴","🤤","😪","😵",
    "👍","👎","👏","🙌","🤝","🤜","🤛","✊","👊","🤚",
    "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔",
    "🔥","✨","⭐","💫","💥","💢","💬","💯","✅","❌"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiReactionSheet(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            // Quick reaction row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QUICK_REACTIONS.forEach { emoji ->
                    QuickEmojiButton(emoji = emoji, onClick = { onEmojiSelected(emoji) })
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            Text(
                "All emojis",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(ALL_EMOJIS) { emoji ->
                    Box(
                        modifier = Modifier.size(40.dp).clickable { onEmojiSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickEmojiButton(emoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 26.sp)
    }
}
