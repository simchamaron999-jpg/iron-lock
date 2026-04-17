package com.mistymessenger.chat.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mistymessenger.chat.viewmodel.ChatsViewModel
import com.mistymessenger.core.ui.components.AvatarImage
import com.mistymessenger.core.ui.components.MessageTickIcon
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatsScreen(
    onChatClick: (String) -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MistyMessenger") },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search") }
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("New group") }, onClick = { showMenu = false; /* navigate */ leadingIcon = null }, leadingIcon = { Icon(Icons.Default.Group, null) })
                        DropdownMenuItem(text = { Text("Broadcast list") }, onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Default.Campaign, null) })
                        DropdownMenuItem(text = { Text("Starred messages") }, onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Default.Star, null) })
                        DropdownMenuItem(text = { Text("Settings") }, onClick = { showMenu = false; onSettings() }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChat) {
                Icon(Icons.Default.Edit, "New chat")
            }
        }
    ) { padding ->
        if (chats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No chats yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap the pencil icon to start a conversation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(chats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        onClick = { onChatClick(chat.id) },
                        onLongClick = { viewModel.onChatLongPress(chat.id) },
                        modifier = Modifier.animateItemPlacement()
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListItem(
    chat: com.mistymessenger.core.db.entity.ChatEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val displayTime = if (chat.lastMessageAt > 0) timeFormat.format(Date(chat.lastMessageAt)) else ""

    ListItem(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(displayTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.lastMessageText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Badge { Text(chat.unreadCount.toString()) }
                }
                if (chat.isMuted) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.VolumeOff, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        },
        leadingContent = {
            AvatarImage(url = chat.avatarUrl, name = chat.name, size = 48.dp)
        }
    )
}
