package com.mistymessenger.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.chat.viewmodel.ContactInfoViewModel
import com.mistymessenger.core.ui.components.AvatarImage
import com.mistymessenger.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInfoScreen(
    userId: String,
    navController: NavHostController,
    viewModel: ContactInfoViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showBlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) { viewModel.load(userId) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Contact Info") },
                actions = {
                    IconButton(onClick = { /* show more options */ }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading && state.name.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Avatar + name header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarImage(url = state.avatarUrl, name = state.name, size = 96.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(state.name, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.bio.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Online / last seen
                    Spacer(Modifier.height(4.dp))
                    if (state.isOnline) {
                        Text("online", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else if (state.lastSeen.isNotBlank()) {
                        Text(
                            "last seen ${formatLastSeen(state.lastSeen)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ContactActionButton(Icons.Default.Chat, "Message") {
                        if (state.chatId.isNotEmpty()) {
                            navController.navigate(Screen.ChatDetail.createRoute(state.chatId))
                        }
                    }
                    ContactActionButton(Icons.Default.Call, "Voice") {
                        if (state.chatId.isNotEmpty()) {
                            navController.navigate(Screen.VoiceCall.createRoute(state.chatId))
                        }
                    }
                    ContactActionButton(Icons.Default.Videocam, "Video") {
                        if (state.chatId.isNotEmpty()) {
                            navController.navigate(Screen.VideoCall.createRoute(state.chatId))
                        }
                    }
                    ContactActionButton(Icons.Default.Search, "Search") {
                        // search messages with this contact
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            // Media shortcut
            item {
                ListItem(
                    headlineContent = { Text("Media, links and docs") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (state.chatId.isNotEmpty()) {
                            navController.navigate(Screen.MediaGallery.createRoute(state.chatId))
                        }
                    }
                )
                HorizontalDivider()
            }

            // Notifications
            item {
                ListItem(
                    headlineContent = { Text("Custom notifications") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                    modifier = Modifier.fillMaxWidth().clickable { /* open notification settings */ }
                )
                HorizontalDivider()
            }

            // Chat lock (WhatsApp Plus feature)
            item {
                ListItem(
                    headlineContent = { Text("Lock chat") },
                    supportingContent = { Text("Require biometrics to open this chat", style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (state.chatId.isNotEmpty()) {
                            navController.navigate(Screen.ChatLock.createRoute(state.chatId))
                        }
                    }
                )
                HorizontalDivider()
            }

            // Block / Unblock
            item {
                Spacer(Modifier.height(16.dp))
                ListItem(
                    headlineContent = {
                        Text(
                            if (state.isBlocked) "Unblock ${state.name}" else "Block ${state.name}",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showBlockDialog = true }
                )
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(if (state.isBlocked) "Unblock ${state.name}?" else "Block ${state.name}?") },
            text = {
                Text(
                    if (state.isBlocked) "You will be able to receive messages from ${state.name}."
                    else "You won't receive messages from ${state.name}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (state.isBlocked) viewModel.unblockContact(userId)
                    else viewModel.blockContact(userId)
                    showBlockDialog = false
                }) { Text(if (state.isBlocked) "Unblock" else "Block", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ContactActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
            Icon(icon, label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatLastSeen(iso: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val date = sdf.parse(iso) ?: return iso
        val diff = System.currentTimeMillis() - date.time
        when {
            diff < 60_000 -> "just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 172800_000 -> "yesterday"
            else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(date)
        }
    } catch (e: Exception) { iso }
}

// Make ListItem clickable (extension)
private fun Modifier.clickable(onClick: () -> Unit) = this.then(
    androidx.compose.foundation.Modifier.clickable(onClick = onClick)
)
