package com.mistymessenger.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.chat.viewmodel.GlobalSearchViewModel
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.core.ui.components.AvatarImage
import com.mistymessenger.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    navController: NavHostController,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.search(it)
                        },
                        placeholder = { Text("Search messages, contacts, chats") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; viewModel.search("") }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Search across all messages and contacts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Chats
                if (state.chats.isNotEmpty()) {
                    item {
                        SectionHeader("Chats", Icons.Default.Chat)
                    }
                    items(state.chats, key = { "chat_${it.id}" }) { chat ->
                        ListItem(
                            modifier = Modifier.clickable { navController.navigate(Screen.ChatDetail.createRoute(chat.id)) },
                            headlineContent = { HighlightedText(chat.name, query) },
                            leadingContent = { AvatarImage(url = chat.avatarUrl, name = chat.name, size = 40.dp) }
                        )
                    }
                }

                // Contacts
                if (state.contacts.isNotEmpty()) {
                    item { SectionHeader("Contacts", Icons.Default.Person) }
                    items(state.contacts, key = { "contact_${it.userId}" }) { contact ->
                        ListItem(
                            modifier = Modifier.clickable {
                                navController.navigate(Screen.ContactInfo.createRoute(contact.userId))
                            },
                            headlineContent = { HighlightedText(contact.name, query) },
                            supportingContent = { Text(contact.phone, style = MaterialTheme.typography.bodySmall) },
                            leadingContent = { AvatarImage(url = contact.avatarUrl, name = contact.name, size = 40.dp) }
                        )
                    }
                }

                // Messages
                if (state.messages.isNotEmpty()) {
                    item { SectionHeader("Messages", Icons.Default.Message) }
                    items(state.messages, key = { "msg_${it.id}" }) { message ->
                        MessageSearchItem(
                            message = message,
                            query = query,
                            onClick = { navController.navigate(Screen.ChatDetail.createRoute(message.chatId)) }
                        )
                    }
                }

                if (state.chats.isEmpty() && state.contacts.isEmpty() && state.messages.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider()
}

@Composable
private fun HighlightedText(text: String, query: String) {
    if (query.isBlank()) {
        Text(text)
        return
    }
    val lower = text.lowercase()
    val queryLower = query.lowercase()
    val idx = lower.indexOf(queryLower)
    if (idx < 0) { Text(text); return }

    val annotated = buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
            append(text.substring(idx, idx + query.length))
        }
        append(text.substring(idx + query.length))
    }
    Text(annotated)
}

@Composable
private fun MessageSearchItem(message: MessageEntity, query: String, onClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = { HighlightedText(message.content, query) },
        supportingContent = {
            Text(
                timeFormat.format(Date(message.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(Icons.Default.Message, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    )
}
