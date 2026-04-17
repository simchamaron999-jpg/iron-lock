package com.mistymessenger.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.chat.viewmodel.StarredMessagesViewModel
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    navController: NavHostController,
    viewModel: StarredMessagesViewModel = hiltViewModel()
) {
    val messages by viewModel.starredMessages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Starred Messages") }
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No starred messages", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Long-press a message and tap Star", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(messages, key = { it.id }) { message ->
                    StarredMessageItem(
                        message = message,
                        onClick = { navController.navigate(Screen.ChatDetail.createRoute(message.chatId)) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StarredMessageItem(message: MessageEntity, onClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = {
            val preview = when {
                message.isDeletedForEveryone -> "This message was deleted"
                message.type == "text" -> message.content
                message.type == "image" -> "Photo"
                message.type == "video" -> "Video"
                message.type == "audio" -> "Voice message"
                message.type == "document" -> "Document"
                else -> message.content
            }
            Text(preview, maxLines = 2)
        },
        supportingContent = {
            Text(
                timeFormat.format(Date(message.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    )
}
