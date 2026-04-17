package com.mistymessenger.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.chat.viewmodel.BroadcastViewModel
import com.mistymessenger.core.ui.components.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    navController: NavHostController,
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    val contacts by viewModel.filteredContacts.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val message by viewModel.message.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val sentCount by viewModel.sentCount.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    Column {
                        Text("Broadcast")
                        if (selected.isNotEmpty()) {
                            Text("${selected.size} recipients",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { viewModel.setMessage(it) },
                    label = { Text("Message to broadcast") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.send { navController.popBackStack() } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending && selected.isNotEmpty() && message.isNotBlank()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Sent to $sentCount/${selected.size}")
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send to ${selected.size} contacts")
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Selected chips
            if (selected.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selected.toList()) { userId ->
                        val contact = contacts.find { it.userId == userId }
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleContact(userId) },
                            label = { Text(contact?.name ?: userId, style = MaterialTheme.typography.labelMedium) },
                            avatar = { AvatarImage(url = contact?.avatarUrl ?: "", name = contact?.name ?: "", size = 24.dp) },
                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                HorizontalDivider()
            }

            // Search bar
            SearchBar(
                query = query,
                onQueryChange = { viewModel.setQuery(it) },
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Search contacts") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {}

            LazyColumn {
                items(contacts, key = { it.userId }) { contact ->
                    ListItem(
                        modifier = Modifier.clickable { viewModel.toggleContact(contact.userId) },
                        headlineContent = { Text(contact.name) },
                        supportingContent = { Text(contact.phone, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = { AvatarImage(url = contact.avatarUrl, name = contact.name, size = 40.dp) },
                        trailingContent = {
                            Checkbox(
                                checked = contact.userId in selected,
                                onCheckedChange = { viewModel.toggleContact(contact.userId) }
                            )
                        }
                    )
                }
            }
        }
    }
}
