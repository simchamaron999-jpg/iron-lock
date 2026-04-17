package com.mistymessenger.chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mistymessenger.chat.viewmodel.ForwardViewModel
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.ui.components.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessageScreen(
    messageId: String,
    onForwarded: () -> Unit,
    onBack: () -> Unit,
    viewModel: ForwardViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val selected by viewModel.selectedIds.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadMessage(messageId) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.onQueryChange(it)
                        },
                        placeholder = { Text("Search chats or contacts") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        },
        floatingActionButton = {
            if (selected.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("Forward (${selected.size})") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, null) },
                    onClick = {
                        viewModel.forwardMessage(onForwarded)
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (chats.isNotEmpty()) {
                item {
                    Text("Recent chats", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                }
                items(chats, key = { it.id }) { chat ->
                    ForwardChatItem(
                        name = chat.name,
                        avatarUrl = chat.avatarUrl,
                        isSelected = chat.id in selected,
                        onToggle = { viewModel.toggleSelection(chat.id) }
                    )
                }
            }
            if (contacts.isNotEmpty()) {
                item {
                    Text("Contacts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                }
                items(contacts, key = { "c_${it.userId}" }) { contact ->
                    ForwardChatItem(
                        name = contact.name,
                        avatarUrl = contact.avatarUrl,
                        isSelected = contact.userId in selected,
                        onToggle = { viewModel.toggleContactSelection(contact.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ForwardChatItem(
    name: String,
    avatarUrl: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    ListItem(
        headlineContent = { Text(name) },
        leadingContent = { AvatarImage(url = avatarUrl, name = name, size = 48.dp) },
        trailingContent = {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        },
        modifier = Modifier.fillMaxWidth()
    )
}
