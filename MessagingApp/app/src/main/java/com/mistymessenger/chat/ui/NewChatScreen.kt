package com.mistymessenger.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mistymessenger.chat.viewmodel.NewChatViewModel
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.ui.components.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onChatSelected: (String) -> Unit,
    onBack: () -> Unit,
    onCreateGroup: (() -> Unit)? = null,
    viewModel: NewChatViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Search contacts") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }
                            }
                        },
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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // New group shortcut
            if (onCreateGroup != null) {
                item {
                    ListItem(
                        modifier = Modifier.clickable { onCreateGroup() },
                        headlineContent = { Text("New group") },
                        leadingContent = {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                    if (contacts.isNotEmpty()) {
                        Text(
                            "Contacts on MistyMessenger",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                }
            }

            if (contacts.isEmpty() && query.isBlank()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No contacts found", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("Contacts are synced from your phone book", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(contacts, key = { it.userId }) { contact ->
                    ContactListItem(
                        contact = contact,
                        onClick = {
                            viewModel.openOrCreateDm(contact.userId) { chatId ->
                                onChatSelected(chatId)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
fun ContactListItem(
    contact: ContactEntity,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(contact.name) },
        supportingContent = {
            Text(
                contact.phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            AvatarImage(url = contact.avatarUrl, name = contact.name, size = 48.dp)
        },
        trailingContent = trailing
    )
}
